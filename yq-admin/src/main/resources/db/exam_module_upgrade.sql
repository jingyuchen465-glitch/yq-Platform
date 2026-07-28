-- YQ exam module upgrade for databases created from the original attachment schema.
-- Run this script once, after taking a database backup. It contains no DROP TABLE statements.
-- The paper-question snapshot backfill deliberately fails when source questions are missing.

ALTER TABLE `exam_question_course`
  DROP PRIMARY KEY,
  ADD COLUMN `id` bigint NOT NULL AUTO_INCREMENT FIRST,
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_exam_question_course_stage` (`question_id`,`course_id`,`stage_name`),
  ADD KEY `idx_exam_question_course_filter` (`course_id`,`stage_name`,`question_id`);

ALTER TABLE `exam_paper`
  ADD COLUMN `status` varchar(32) NOT NULL DEFAULT 'DRAFT' AFTER `total_score`,
  ADD COLUMN `created_by` bigint DEFAULT NULL AFTER `status`,
  MODIFY COLUMN `total_score` decimal(10,2) NOT NULL;

CREATE TABLE IF NOT EXISTS `exam_paper_stage` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paper_id` bigint NOT NULL,
  `stage_name` varchar(64) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_paper_stage` (`paper_id`,`stage_name`),
  KEY `idx_exam_paper_stage_paper` (`paper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Paper stage coverage';

INSERT IGNORE INTO `exam_paper_stage` (`paper_id`,`stage_name`,`created_at`)
SELECT `id`,`stage_name`,COALESCE(`created_at`,NOW())
FROM `exam_paper`
WHERE `stage_name` IS NOT NULL AND TRIM(`stage_name`) <> '';

ALTER TABLE `exam_paper`
  DROP INDEX `idx_exam_paper_course_stage`,
  DROP COLUMN `stage_name`,
  ADD KEY `idx_exam_paper_course_status` (`course_id`,`status`);

ALTER TABLE `exam_paper_question`
  ADD COLUMN `question_type` varchar(32) DEFAULT NULL AFTER `question_id`,
  ADD COLUMN `question_content` text DEFAULT NULL AFTER `question_type`,
  ADD COLUMN `answer_content` text DEFAULT NULL AFTER `question_content`,
  ADD COLUMN `analysis_content` text DEFAULT NULL AFTER `answer_content`,
  ADD COLUMN `difficulty` varchar(32) DEFAULT NULL AFTER `analysis_content`,
  ADD COLUMN `sort_order` int NOT NULL DEFAULT 0 AFTER `question_score`,
  MODIFY COLUMN `question_score` decimal(10,2) NOT NULL;

UPDATE `exam_paper_question` pq
JOIN `exam_question` q ON q.`id` = pq.`question_id`
SET pq.`question_type` = q.`question_type`,
    pq.`question_content` = q.`question_content`,
    pq.`answer_content` = q.`answer_content`,
    pq.`analysis_content` = q.`analysis_content`,
    pq.`difficulty` = q.`difficulty`,
    pq.`sort_order` = pq.`id`;

-- This ALTER stops safely if an old paper references a missing source question.
ALTER TABLE `exam_paper_question`
  MODIFY COLUMN `question_type` varchar(32) NOT NULL,
  MODIFY COLUMN `question_content` text NOT NULL,
  MODIFY COLUMN `answer_content` text NOT NULL,
  MODIFY COLUMN `difficulty` varchar(32) NOT NULL,
  ADD KEY `idx_exam_paper_question_order` (`paper_id`,`sort_order`,`id`);

CREATE TABLE IF NOT EXISTS `exam_paper_question_option` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paper_question_id` bigint NOT NULL,
  `option_key` varchar(16) NOT NULL,
  `option_content` text NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_paper_question_option` (`paper_question_id`,`option_key`),
  KEY `idx_exam_paper_question_option_question` (`paper_question_id`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Paper option snapshots';

INSERT IGNORE INTO `exam_paper_question_option`
  (`paper_question_id`,`option_key`,`option_content`,`sort_order`,`created_at`)
SELECT pq.`id`,qo.`option_key`,qo.`option_content`,qo.`sort_order`,COALESCE(pq.`created_at`,NOW())
FROM `exam_paper_question` pq
JOIN `exam_question_option` qo ON qo.`question_id` = pq.`question_id`;

ALTER TABLE `exam`
  CHANGE COLUMN `end_time` `entry_deadline_time` datetime NOT NULL,
  ADD COLUMN `close_time` datetime DEFAULT NULL AFTER `duration_minutes`,
  MODIFY COLUMN `invigilator_user_id` bigint DEFAULT NULL,
  ADD COLUMN `answer_visible_at` datetime DEFAULT NULL AFTER `answer_visible`,
  ADD COLUMN `created_by` bigint DEFAULT NULL AFTER `answer_visible_at`,
  ADD UNIQUE KEY `uk_exam_publish` (`paper_id`,`class_id`,`start_time`),
  ADD KEY `idx_exam_close_time` (`close_time`);

UPDATE `exam`
SET `close_time` = DATE_ADD(`entry_deadline_time`, INTERVAL `duration_minutes` MINUTE);

ALTER TABLE `exam`
  MODIFY COLUMN `close_time` datetime NOT NULL;

UPDATE `exam_paper` p
SET p.`status` = 'LOCKED'
WHERE EXISTS (SELECT 1 FROM `exam` e WHERE e.`paper_id` = p.`id`);

ALTER TABLE `student_exam_record`
  MODIFY COLUMN `start_time` datetime DEFAULT NULL,
  MODIFY COLUMN `deadline_time` datetime DEFAULT NULL,
  ADD COLUMN `submit_reason` varchar(32) DEFAULT NULL AFTER `submit_time`,
  ADD COLUMN `objective_score` decimal(10,2) NOT NULL DEFAULT 0 AFTER `grading_status`,
  ADD COLUMN `subjective_score` decimal(10,2) NOT NULL DEFAULT 0 AFTER `objective_score`,
  ADD COLUMN `version` int NOT NULL DEFAULT 0 AFTER `score`,
  ADD KEY `idx_student_exam_record_deadline` (`status`,`deadline_time`);

UPDATE `student_exam_record`
SET `objective_score` = COALESCE(`score`,0)
WHERE `status` IN ('SUBMITTED','TIMEOUT');

ALTER TABLE `student_exam_answer`
  ADD COLUMN `paper_question_id` bigint DEFAULT NULL AFTER `paper_id`,
  ADD COLUMN `grader_user_id` bigint DEFAULT NULL AFTER `score`,
  ADD COLUMN `grader_comment` varchar(500) DEFAULT NULL AFTER `grader_user_id`,
  ADD COLUMN `graded_at` datetime DEFAULT NULL AFTER `grader_comment`;

UPDATE `student_exam_answer` a
JOIN `exam_paper_question` pq
  ON pq.`paper_id` = a.`paper_id` AND pq.`question_id` = a.`question_id`
SET a.`paper_question_id` = pq.`id`;

-- This ALTER stops safely when an old answer cannot be mapped to a paper snapshot.
ALTER TABLE `student_exam_answer`
  DROP INDEX `uk_record_question`,
  MODIFY COLUMN `paper_question_id` bigint NOT NULL,
  ADD UNIQUE KEY `uk_record_paper_question` (`record_id`,`paper_question_id`),
  ADD KEY `idx_student_exam_answer_paper_question` (`paper_question_id`);

CREATE TABLE IF NOT EXISTS `exam_timeout_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint NOT NULL,
  `deadline_time` datetime NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `publish_attempts` int NOT NULL DEFAULT 0,
  `published_at` datetime DEFAULT NULL,
  `processed_at` datetime DEFAULT NULL,
  `last_error` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_timeout_record` (`record_id`),
  KEY `idx_exam_timeout_outbox_status` (`status`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Exam timeout outbox';
