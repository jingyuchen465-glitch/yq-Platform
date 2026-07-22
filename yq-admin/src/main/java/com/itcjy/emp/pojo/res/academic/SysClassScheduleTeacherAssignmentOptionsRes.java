package com.itcjy.emp.pojo.res.academic;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "课表阶段教师分配选项")
public record SysClassScheduleTeacherAssignmentOptionsRes(
        @Schema(description = "可分配的课程阶段") List<StageOption> stages,
        @Schema(description = "讲师选项，仅包含有效的LECTURER角色用户") List<TeacherOption> teachers
) {

    @Schema(description = "课程阶段选项")
    public record StageOption(
            @Schema(description = "课程阶段") String stageName,
            @Schema(description = "该阶段实际上课日期") List<LocalDate> classDates,
            @Schema(description = "当前统一分配的教师ID") Long teacherId,
            @Schema(description = "当前教师名称，存在多位教师时返回“多位教师”") String teacherName
    ) {
    }

    @Schema(description = "授课教师选项")
    public record TeacherOption(
            @Schema(description = "教师ID") Long id,
            @Schema(description = "教师显示名称") String label,
            @Schema(description = "该教师在其他班级已有课程的日期") List<LocalDate> occupiedDates
    ) {
    }
}
