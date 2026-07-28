-- YQ 题库中文测试数据。
--
-- 特点：
-- 1. 不删除、不覆盖现有题目，可重复执行。
-- 2. 覆盖 SINGLE、MULTIPLE、JUDGE、FILL、SHORT 五种题型。
-- 3. 覆盖 EASY、NORMAL、HARD 三种难度以及 ENABLED、DISABLED 两种状态。
-- 4. 课程和阶段通过名称匹配现有 sys_course、sys_course_detail 数据。

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS `tmp_exam_question_seed`;
CREATE TEMPORARY TABLE `tmp_exam_question_seed` (
  `seed_code` varchar(32) NOT NULL,
  `question_type` varchar(32) NOT NULL,
  `question_content` text NOT NULL,
  `answer_content` text NOT NULL,
  `analysis_content` text,
  `difficulty` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  PRIMARY KEY (`seed_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tmp_exam_question_seed`
(`seed_code`, `question_type`, `question_content`, `answer_content`, `analysis_content`, `difficulty`, `status`)
VALUES
('YQ-Q001', 'SINGLE', '在 Java 中，哪个关键字用于声明一个类继承另一个类？', 'A', 'Java 使用 extends 关键字表示类继承；implements 用于实现接口。', 'EASY', 'ENABLED'),
('YQ-Q002', 'MULTIPLE', '下列哪些属于 Java 面向对象的基本特征？', 'A,C,D', '面向对象的基本特征通常包括封装、继承和多态。编译是一种程序处理过程，不属于面向对象特征。', 'EASY', 'ENABLED'),
('YQ-Q003', 'JUDGE', 'Java 中的 String 对象创建后其内容不可变。', 'TRUE', 'String 是不可变类，字符串内容变化时会产生新的 String 对象。', 'EASY', 'ENABLED'),
('YQ-Q004', 'FILL', 'Java 程序的标准入口方法签名是 ______。', 'public static void main(String[] args)', 'JVM 从 public static void main(String[] args) 方法开始执行普通 Java 应用程序。', 'NORMAL', 'ENABLED'),
('YQ-Q005', 'SHORT', '请简述 Java 接口与抽象类的主要区别。', '接口更侧重行为契约，一个类可以实现多个接口；抽象类可同时包含状态、构造方法和具体实现，一个类只能直接继承一个抽象类。', '可从设计用途、成员能力和继承数量三个方面回答。', 'NORMAL', 'ENABLED'),
('YQ-Q006', 'SINGLE', 'MyBatis-Plus 的 BaseMapper 中，按主键查询单条数据应调用哪个方法？', 'B', 'BaseMapper.selectById(id) 用于按主键查询一条记录。', 'EASY', 'ENABLED'),
('YQ-Q007', 'MULTIPLE', '下列哪些做法有助于降低数据库查询中的 N+1 问题？', 'A,C,D', '批量 IN 查询、合理 JOIN 和分批加载关联数据都能减少逐行查询；在循环中逐条查询会造成 N+1。', 'NORMAL', 'ENABLED'),
('YQ-Q008', 'JUDGE', '只要被 @Transactional 标注的方法抛出任意异常，Spring 默认都会回滚事务。', 'FALSE', 'Spring 默认对 RuntimeException 和 Error 回滚；受检异常通常需要通过 rollbackFor 显式配置。', 'HARD', 'ENABLED'),
('YQ-Q009', 'FILL', 'HTTP 状态码中，表示服务器成功创建资源的是 ______。', '201', '201 Created 表示请求成功，并且服务器创建了新的资源。', 'EASY', 'ENABLED'),
('YQ-Q010', 'SHORT', '请说明 REST 接口中幂等性的含义，并举出一种常见的幂等请求方法。', '幂等是指同一请求执行一次或重复执行多次，对服务器资源产生的最终效果一致。PUT 通常应设计为幂等请求。', '答案应包含“重复执行最终效果一致”，并举出 PUT 或 DELETE 等合理示例。', 'NORMAL', 'ENABLED'),
('YQ-Q011', 'SINGLE', '在 Vue 2 中，用于实现表单输入双向绑定的指令是？', 'C', 'v-model 用于在表单控件或组件上创建双向数据绑定。', 'EASY', 'ENABLED'),
('YQ-Q012', 'MULTIPLE', '下列哪些属于 Vue 2 组件常用的生命周期钩子？', 'A,B,C', 'created、mounted 和 destroyed 都是 Vue 2 生命周期钩子；computed 是计算属性配置项。', 'NORMAL', 'ENABLED'),
('YQ-Q013', 'JUDGE', 'Vue 的计算属性会基于其响应式依赖进行缓存。', 'TRUE', '计算属性只有在相关响应式依赖发生变化时才会重新求值。', 'EASY', 'ENABLED'),
('YQ-Q014', 'FILL', '使用 Axios 发起 GET 请求时，常用的方法调用是 ______。', 'axios.get', 'Axios 提供 axios.get(url, config) 方法发送 GET 请求。', 'EASY', 'ENABLED'),
('YQ-Q015', 'SHORT', '请简述前端防抖的作用，并说明一个适用场景。', '防抖会在事件连续触发时重新计时，只有停止触发达到指定时间后才执行目标函数，常用于搜索框输入联想、窗口尺寸变化等高频事件。', '重点是“连续触发只执行最后一次”，场景合理即可。', 'NORMAL', 'ENABLED'),
('YQ-Q016', 'SINGLE', 'RocketMQ 消费者重复收到同一业务消息时，最重要的处理原则是？', 'D', '消息系统通常采用至少一次投递，消费者必须保证业务处理幂等。', 'NORMAL', 'ENABLED'),
('YQ-Q017', 'MULTIPLE', '下列哪些场景适合使用消息队列？', 'A,B,C', '消息队列适合异步解耦、流量削峰和最终一致性；不能替代所有强一致本地事务。', 'NORMAL', 'ENABLED'),
('YQ-Q018', 'JUDGE', '无状态服务通常比保存本地会话状态的服务更便于水平扩展。', 'TRUE', '无状态实例之间更容易互相替换，请求也更容易被负载均衡到任意实例。', 'EASY', 'ENABLED'),
('YQ-Q019', 'SHORT', '请设计学生考试超时自动交卷的可靠性方案，至少说明消息、幂等和补偿三个方面。', '开始考试时在同一事务中写入超时 Outbox，并投递包含记录 ID 和截止时间的延时消息；消费者通过条件更新仅处理仍处于答题中且已到期的记录；定时任务扫描到期记录和发送失败的 Outbox 进行补偿，主动提交与超时提交通过乐观锁或条件更新竞争。', '应覆盖可靠投递、重复消费幂等、失败补偿以及并发封卷控制。', 'HARD', 'ENABLED'),
('YQ-Q020', 'MULTIPLE', '发布考试时，为避免学生后续转班影响已发布考试范围，应保存哪些快照信息？', 'A,B,C', '发布时应固化班级、学生名单以及试卷题目内容。浏览器信息不决定考试发布范围。', 'HARD', 'ENABLED'),
('YQ-Q021', 'SINGLE', 'SQL 中按指定条件删除表内数据应使用哪个关键字？', 'B', 'DELETE 可以配合 WHERE 条件删除指定记录。本题设置为停用，用于验证题库状态筛选。', 'EASY', 'DISABLED');

INSERT INTO `exam_question`
(`question_type`, `question_content`, `answer_content`, `analysis_content`, `difficulty`, `status`, `created_at`, `updated_at`)
SELECT s.`question_type`, s.`question_content`, s.`answer_content`, s.`analysis_content`,
       s.`difficulty`, s.`status`, NOW(), NOW()
FROM `tmp_exam_question_seed` s
WHERE NOT EXISTS (
  SELECT 1
  FROM `exam_question` q
  WHERE q.`question_type` = s.`question_type`
    AND q.`question_content` = s.`question_content`
);

DROP TEMPORARY TABLE IF EXISTS `tmp_exam_question_map`;
CREATE TEMPORARY TABLE `tmp_exam_question_map` (
  `seed_code` varchar(32) NOT NULL,
  `question_id` bigint NOT NULL,
  PRIMARY KEY (`seed_code`),
  UNIQUE KEY `uk_tmp_exam_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tmp_exam_question_map` (`seed_code`, `question_id`)
SELECT s.`seed_code`, MIN(q.`id`)
FROM `tmp_exam_question_seed` s
JOIN `exam_question` q
  ON q.`question_type` = s.`question_type`
 AND q.`question_content` = s.`question_content`
GROUP BY s.`seed_code`;

DROP TEMPORARY TABLE IF EXISTS `tmp_exam_option_seed`;
CREATE TEMPORARY TABLE `tmp_exam_option_seed` (
  `seed_code` varchar(32) NOT NULL,
  `option_key` varchar(16) NOT NULL,
  `option_content` text NOT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`seed_code`, `option_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tmp_exam_option_seed`
(`seed_code`, `option_key`, `option_content`, `sort_order`)
VALUES
('YQ-Q001', 'A', 'extends', 1), ('YQ-Q001', 'B', 'implements', 2), ('YQ-Q001', 'C', 'import', 3), ('YQ-Q001', 'D', 'package', 4),
('YQ-Q002', 'A', '封装', 1), ('YQ-Q002', 'B', '编译', 2), ('YQ-Q002', 'C', '继承', 3), ('YQ-Q002', 'D', '多态', 4),
('YQ-Q006', 'A', 'findById', 1), ('YQ-Q006', 'B', 'selectById', 2), ('YQ-Q006', 'C', 'queryByPrimaryKey', 3), ('YQ-Q006', 'D', 'getOneById', 4),
('YQ-Q007', 'A', '先分页查询主表 ID，再使用 IN 批量查询关联数据', 1), ('YQ-Q007', 'B', '遍历每一行主数据并单独查询关联数据', 2), ('YQ-Q007', 'C', '在合适场景使用 JOIN 一次查询所需数据', 3), ('YQ-Q007', 'D', '按关联类型批量加载后在内存中组装', 4),
('YQ-Q011', 'A', 'v-if', 1), ('YQ-Q011', 'B', 'v-for', 2), ('YQ-Q011', 'C', 'v-model', 3), ('YQ-Q011', 'D', 'v-once', 4),
('YQ-Q012', 'A', 'created', 1), ('YQ-Q012', 'B', 'mounted', 2), ('YQ-Q012', 'C', 'destroyed', 3), ('YQ-Q012', 'D', 'computed', 4),
('YQ-Q016', 'A', '每次都重新创建一条业务数据', 1), ('YQ-Q016', 'B', '忽略所有重复消息和首次消息', 2), ('YQ-Q016', 'C', '无限重试且不记录处理状态', 3), ('YQ-Q016', 'D', '依据业务唯一键保证消费幂等', 4),
('YQ-Q017', 'A', '异步解耦非实时业务流程', 1), ('YQ-Q017', 'B', '突发流量削峰', 2), ('YQ-Q017', 'C', '跨服务最终一致性', 3), ('YQ-Q017', 'D', '替代所有需要强一致性的本地数据库事务', 4),
('YQ-Q020', 'A', '发布目标班级', 1), ('YQ-Q020', 'B', '当时的学生名单', 2), ('YQ-Q020', 'C', '试卷题目及选项', 3), ('YQ-Q020', 'D', '学生当前浏览器版本', 4),
('YQ-Q021', 'A', 'DROP', 1), ('YQ-Q021', 'B', 'DELETE', 2), ('YQ-Q021', 'C', 'ALTER', 3), ('YQ-Q021', 'D', 'GRANT', 4);

INSERT INTO `exam_question_option`
(`question_id`, `option_key`, `option_content`, `sort_order`, `created_at`, `updated_at`)
SELECT m.`question_id`, o.`option_key`, o.`option_content`, o.`sort_order`, NOW(), NOW()
FROM `tmp_exam_option_seed` o
JOIN `tmp_exam_question_map` m ON m.`seed_code` = o.`seed_code`
WHERE NOT EXISTS (
  SELECT 1
  FROM `exam_question_option` qo
  WHERE qo.`question_id` = m.`question_id`
    AND qo.`option_key` = o.`option_key`
);

DROP TEMPORARY TABLE IF EXISTS `tmp_exam_relation_seed`;
CREATE TEMPORARY TABLE `tmp_exam_relation_seed` (
  `seed_code` varchar(32) NOT NULL,
  `course_name` varchar(128) NOT NULL,
  `stage_name` varchar(64) NOT NULL,
  PRIMARY KEY (`seed_code`, `course_name`, `stage_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tmp_exam_relation_seed` (`seed_code`, `course_name`, `stage_name`)
VALUES
('YQ-Q001', 'AI智能体开发', '阶段一：夯实基础与编程思维'),
('YQ-Q002', 'AI智能体开发', '阶段一：夯实基础与编程思维'),
('YQ-Q003', 'AI智能体开发', '阶段一：夯实基础与编程思维'),
('YQ-Q004', 'AI智能体开发', '阶段一：夯实基础与编程思维'),
('YQ-Q005', 'AI智能体开发', '阶段一：夯实基础与编程思维'),
('YQ-Q005', 'AI智能体开发', '阶段二：后端核心与数据存储'),
('YQ-Q006', 'AI智能体开发', '阶段二：后端核心与数据存储'),
('YQ-Q007', 'AI智能体开发', '阶段二：后端核心与数据存储'),
('YQ-Q008', 'AI智能体开发', '阶段二：后端核心与数据存储'),
('YQ-Q009', 'AI智能体开发', '阶段二：后端核心与数据存储'),
('YQ-Q010', 'AI智能体开发', '阶段二：后端核心与数据存储'),
('YQ-Q011', 'AI智能体开发', '阶段三：前端交互与全栈融合'),
('YQ-Q012', 'AI智能体开发', '阶段三：前端交互与全栈融合'),
('YQ-Q013', 'AI智能体开发', '阶段三：前端交互与全栈融合'),
('YQ-Q014', 'AI智能体开发', '阶段三：前端交互与全栈融合'),
('YQ-Q015', 'AI智能体开发', '阶段三：前端交互与全栈融合'),
('YQ-Q016', 'AI智能体开发', '阶段四：架构设计、运维与毕业设计'),
('YQ-Q017', 'AI智能体开发', '阶段四：架构设计、运维与毕业设计'),
('YQ-Q018', 'AI智能体开发', '阶段四：架构设计、运维与毕业设计'),
('YQ-Q019', 'AI智能体开发', '阶段四：架构设计、运维与毕业设计'),
('YQ-Q020', 'AI智能体开发', '阶段四：架构设计、运维与毕业设计'),
('YQ-Q001', 'java+ai', '阶段一：基础入门'),
('YQ-Q002', 'java+ai', '阶段一：基础入门'),
('YQ-Q003', 'java+ai', '阶段一：基础入门'),
('YQ-Q004', 'java+ai', '阶段一：基础入门'),
('YQ-Q006', 'java+ai', '阶段二：进阶提升'),
('YQ-Q007', 'java+ai', '阶段二：进阶提升'),
('YQ-Q008', 'java+ai', '阶段二：进阶提升'),
('YQ-Q021', 'java+ai', '阶段二：进阶提升'),
('YQ-Q016', 'java+ai', '阶段三：实战演练'),
('YQ-Q017', 'java+ai', '阶段三：实战演练'),
('YQ-Q018', 'java+ai', '阶段三：实战演练'),
('YQ-Q019', 'java+ai', '阶段三：实战演练');

INSERT INTO `exam_question_course`
(`question_id`, `course_id`, `stage_name`, `created_at`)
SELECT DISTINCT m.`question_id`, c.`id`, r.`stage_name`, NOW()
FROM `tmp_exam_relation_seed` r
JOIN `tmp_exam_question_map` m ON m.`seed_code` = r.`seed_code`
JOIN `sys_course` c ON c.`course_name` = r.`course_name`
WHERE EXISTS (
  SELECT 1
  FROM `sys_course_detail` cd
  WHERE cd.`course_id` = c.`id`
    AND cd.`stage_name` = r.`stage_name`
)
AND NOT EXISTS (
  SELECT 1
  FROM `exam_question_course` qc
  WHERE qc.`question_id` = m.`question_id`
    AND qc.`course_id` = c.`id`
    AND qc.`stage_name` = r.`stage_name`
);

DROP TEMPORARY TABLE IF EXISTS `tmp_exam_relation_seed`;
DROP TEMPORARY TABLE IF EXISTS `tmp_exam_option_seed`;
DROP TEMPORARY TABLE IF EXISTS `tmp_exam_question_map`;
DROP TEMPORARY TABLE IF EXISTS `tmp_exam_question_seed`;

COMMIT;
