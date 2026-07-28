-- YQ exam module baseline schema.
-- This script is intentionally non-destructive and is safe for a fresh database.

CREATE TABLE IF NOT EXISTS `exam_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_type` varchar(32) NOT NULL COMMENT 'SINGLE/MULTIPLE/JUDGE/FILL/SHORT',
  `question_content` text NOT NULL,
  `answer_content` text NOT NULL,
  `analysis_content` text DEFAULT NULL,
  `difficulty` varchar(32) NOT NULL DEFAULT 'NORMAL' COMMENT 'EASY/NORMAL/HARD',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_exam_question_type` (`question_type`),
  KEY `idx_exam_question_status` (`status`),
  KEY `idx_exam_question_difficulty` (`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Exam question bank';

CREATE TABLE IF NOT EXISTS `exam_question_option` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL,
  `option_key` varchar(16) NOT NULL,
  `option_content` text NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_question_option_key` (`question_id`,`option_key`),
  KEY `idx_exam_question_option_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Question options';

CREATE TABLE IF NOT EXISTS `exam_question_course` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `stage_name` varchar(64) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_question_course_stage` (`question_id`,`course_id`,`stage_name`),
  KEY `idx_exam_question_course_filter` (`course_id`,`stage_name`,`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Question course-stage relations';

CREATE TABLE IF NOT EXISTS `exam_paper` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paper_name` varchar(128) NOT NULL,
  `course_id` bigint NOT NULL,
  `total_score` decimal(10,2) NOT NULL DEFAULT 0,
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/LOCKED/ARCHIVED',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_exam_paper_course_status` (`course_id`,`status`),
  KEY `idx_exam_paper_name` (`paper_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Exam papers';

CREATE TABLE IF NOT EXISTS `exam_paper_stage` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paper_id` bigint NOT NULL,
  `stage_name` varchar(64) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_paper_stage` (`paper_id`,`stage_name`),
  KEY `idx_exam_paper_stage_paper` (`paper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Paper stage coverage';

CREATE TABLE IF NOT EXISTS `exam_paper_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paper_id` bigint NOT NULL,
  `question_id` bigint NOT NULL COMMENT 'Source question id',
  `question_type` varchar(32) NOT NULL,
  `question_content` text NOT NULL,
  `answer_content` text NOT NULL,
  `analysis_content` text DEFAULT NULL,
  `difficulty` varchar(32) NOT NULL,
  `question_score` decimal(10,2) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_paper_question` (`paper_id`,`question_id`),
  KEY `idx_exam_paper_question_order` (`paper_id`,`sort_order`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Paper question snapshots';

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

CREATE TABLE IF NOT EXISTS `exam` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paper_id` bigint NOT NULL,
  `class_id` bigint NOT NULL,
  `start_time` datetime NOT NULL,
  `entry_deadline_time` datetime NOT NULL,
  `duration_minutes` int NOT NULL,
  `close_time` datetime NOT NULL,
  `invigilator_user_id` bigint DEFAULT NULL,
  `answer_visible` tinyint(1) NOT NULL DEFAULT 0,
  `answer_visible_at` datetime DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_publish` (`paper_id`,`class_id`,`start_time`),
  KEY `idx_exam_class_time` (`class_id`,`start_time`,`entry_deadline_time`),
  KEY `idx_exam_close_time` (`close_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Published exams';

CREATE TABLE IF NOT EXISTS `student_exam_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exam_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `start_time` datetime DEFAULT NULL,
  `deadline_time` datetime DEFAULT NULL,
  `submit_time` datetime DEFAULT NULL,
  `submit_reason` varchar(32) DEFAULT NULL COMMENT 'MANUAL/TIMEOUT/ABSENT',
  `status` varchar(32) NOT NULL DEFAULT 'NOT_STARTED',
  `grading_status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `objective_score` decimal(10,2) NOT NULL DEFAULT 0,
  `subjective_score` decimal(10,2) NOT NULL DEFAULT 0,
  `score` decimal(10,2) DEFAULT NULL,
  `version` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_student` (`exam_id`,`student_id`),
  KEY `idx_student_exam_record_student` (`student_id`,`status`),
  KEY `idx_student_exam_record_exam` (`exam_id`,`grading_status`),
  KEY `idx_student_exam_record_deadline` (`status`,`deadline_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Student exam records';

CREATE TABLE IF NOT EXISTS `student_exam_answer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint NOT NULL,
  `exam_id` bigint NOT NULL,
  `paper_id` bigint NOT NULL,
  `paper_question_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `question_type` varchar(32) NOT NULL,
  `question_score` decimal(10,2) NOT NULL,
  `answer_content` text DEFAULT NULL,
  `correct` tinyint(1) DEFAULT NULL,
  `score` decimal(10,2) DEFAULT NULL,
  `grader_user_id` bigint DEFAULT NULL,
  `grader_comment` varchar(500) DEFAULT NULL,
  `graded_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_paper_question` (`record_id`,`paper_question_id`),
  KEY `idx_student_exam_answer_record` (`record_id`),
  KEY `idx_student_exam_answer_exam` (`exam_id`),
  KEY `idx_student_exam_answer_paper_question` (`paper_question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Student answers';

CREATE TABLE IF NOT EXISTS `exam_timeout_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint NOT NULL,
  `deadline_time` datetime NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/PROCESSED',
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
