package com.itcjy.emp.pojo.enums.exam;

/**
 * 考试记录状态枚举
 */
public enum ExamRecordStatus {
    /** 未开始 */
    NOT_STARTED,
    /** 进行中 */
    IN_PROGRESS,
    /** 已交卷 */
    SUBMITTED,
    /** 超时自动交卷 */
    TIMEOUT,
    /** 缺考 */
    ABSENT
}
