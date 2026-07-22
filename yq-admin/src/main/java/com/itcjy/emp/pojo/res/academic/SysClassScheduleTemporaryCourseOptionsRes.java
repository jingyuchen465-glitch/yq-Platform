package com.itcjy.emp.pojo.res.academic;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "临时加课表单选项")
public record SysClassScheduleTemporaryCourseOptionsRes(
        @Schema(description = "当前班级课程的阶段选项") List<StageOption> stages,
        @Schema(description = "有效讲师，仅包含LECTURER角色用户") List<TeacherOption> teachers
) {

    @Schema(description = "课程阶段选项")
    public record StageOption(
            @Schema(description = "课程阶段") String stageName,
            @Schema(description = "下拉框显示名称") String label,
            @Schema(description = "该阶段课程数量") Integer courseCount
    ) {
    }

    @Schema(description = "授课教师选项")
    public record TeacherOption(
            @Schema(description = "教师ID") Long id,
            @Schema(description = "教师显示名称") String label
    ) {
    }
}
