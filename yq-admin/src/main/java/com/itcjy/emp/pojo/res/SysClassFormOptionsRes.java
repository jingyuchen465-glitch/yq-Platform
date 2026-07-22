package com.itcjy.emp.pojo.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "班级表单下拉选项集合")
public record SysClassFormOptionsRes(
        @Schema(description = "校区选项") List<SysClassOptionRes> campuses,
        @Schema(description = "班主任选项，仅包含COORDINATOR角色用户") List<SysClassOptionRes> headTeachers,
        @Schema(description = "课程选项，仅包含线下课程") List<SysClassOptionRes> courses
) {
}
