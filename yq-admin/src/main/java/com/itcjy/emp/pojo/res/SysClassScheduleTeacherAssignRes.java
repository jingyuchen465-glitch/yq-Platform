package com.itcjy.emp.pojo.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "按课程阶段分配授课教师结果")
public record SysClassScheduleTeacherAssignRes(
        @Schema(description = "班级ID") Long classId,
        @Schema(description = "课程阶段") String stageName,
        @Schema(description = "授课教师ID") Long teacherId,
        @Schema(description = "授课教师姓名") String teacherName,
        @Schema(description = "阶段开始日期") LocalDate startDate,
        @Schema(description = "阶段结束日期") LocalDate endDate,
        @Schema(description = "更新的上课日数量") Integer assignedClassDays
) {
}
