CREATE TABLE IF NOT EXISTS `course_homework_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '课程作业标准ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `teaching_mode` varchar(20) NOT NULL COMMENT '上课方式：ONLINE线上，OFFLINE线下',
  `stage_name` varchar(64) DEFAULT NULL COMMENT '阶段名称，线上课程使用',
  `day_number` int DEFAULT NULL COMMENT '第几天，线下课程使用',
  `content_object_key` varchar(500) NOT NULL COMMENT '作业标准文档对象Key',
  `content_file_name` varchar(255) NOT NULL COMMENT '作业标准文档文件名',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE生效，INACTIVE失效',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_stage` (`course_id`,`teaching_mode`,`stage_name`),
  UNIQUE KEY `uk_course_day` (`course_id`,`teaching_mode`,`day_number`),
  KEY `idx_course_homework_template_course` (`course_id`),
  KEY `idx_course_homework_template_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程作业标准/训练集表';
