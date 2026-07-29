package com.itcjy.emp.pojo.enums.exam;

/**
 * 考试超时发件箱消息状态枚举
 */
public enum ExamTimeoutOutboxStatus {
    /** 待发送 */
    PENDING,
    /** 已发送到 MQ */
    SENT,
    /** 已被消费处理 */
    PROCESSED
}
