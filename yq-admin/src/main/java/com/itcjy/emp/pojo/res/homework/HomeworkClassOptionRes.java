package com.itcjy.emp.pojo.res.homework;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "作业班级下拉选项")
public record HomeworkClassOptionRes(
        @Schema(description = "班级ID") Long id,
        @Schema(description = "班级期数") String className
) {
}
