package com.itcjy.emp.pojo.enums.exam;

/**
 * 试卷状态枚举
 */
public enum PaperStatus {
    /** 草稿（可编辑） */
    DRAFT,
    /** 已锁定（已发布过，不可编辑） */
    LOCKED,
    /** 已归档 */
    ARCHIVED
}
