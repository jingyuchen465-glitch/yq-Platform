package com.itcjy.emp.pojo.enums.exam;

public enum QuestionType {
    SINGLE, MULTIPLE, JUDGE, FILL, SHORT;

    public boolean objective() {
        return this == SINGLE || this == MULTIPLE || this == JUDGE;
    }

    public boolean choice() {
        return this == SINGLE || this == MULTIPLE;
    }
}
