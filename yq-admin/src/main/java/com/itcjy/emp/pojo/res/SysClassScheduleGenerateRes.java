package com.itcjy.emp.pojo.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "班级课程表生成结果")
public record SysClassScheduleGenerateRes(
        @Schema(description = "班级ID") Long classId,
        @Schema(description = "班级期数") String classPeriod,
        @Schema(description = "课表开始日期") LocalDate startDate,
        @Schema(description = "课表结束日期") LocalDate endDate,
        @Schema(description = "课表总天数") Integer totalDays,
        @Schema(description = "实际上课天数") Integer classDays
) {
}
