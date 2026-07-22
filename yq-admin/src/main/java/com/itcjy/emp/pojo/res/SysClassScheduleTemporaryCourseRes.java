package com.itcjy.emp.pojo.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "临时加课结果")
public record SysClassScheduleTemporaryCourseRes(
        @Schema(description = "班级ID") Long classId,
        @Schema(description = "加课日期") LocalDate scheduleDate,
        @Schema(description = "课程阶段") String stageName,
        @Schema(description = "课程内容") String courseContent,
        @Schema(description = "授课教师ID") Long teacherId,
        @Schema(description = "授课教师姓名") String teacherName,
        @Schema(description = "目标日原本是否已有课程") Boolean shifted,
        @Schema(description = "被顺延的原课程数量") Integer shiftedClassCount,
        @Schema(description = "调整后的课表结束日期") LocalDate endDate
) {
}
