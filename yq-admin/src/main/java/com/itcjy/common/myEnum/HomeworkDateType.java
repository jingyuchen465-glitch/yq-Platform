package com.itcjy.common.myEnum;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "作业情况查询日期类型")
public enum HomeworkDateType {
    HOMEWORK_DATE,
    DEADLINE_DATE
}
