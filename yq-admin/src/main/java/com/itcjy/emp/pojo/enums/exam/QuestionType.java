package com.itcjy.emp.pojo.enums.exam;

/**
 * 题型枚举
 */
public enum QuestionType {
    /** 单选题 */
    SINGLE,
    /** 多选题 */
    MULTIPLE,
    /** 判断题 */
    JUDGE,
    /** 填空题 */
    FILL,
    /** 简答题 */
    SHORT;

    /**
     * 是否为客观题（可自动判分）
     */
    public boolean objective() {
        return this == SINGLE || this == MULTIPLE || this == JUDGE;
    }

    /**
     * 是否为选择题（拥有选项）
     */
    public boolean choice() {
        return this == SINGLE || this == MULTIPLE;
    }
}
