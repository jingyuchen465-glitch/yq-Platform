package com.itcjy.emp.pojo.res.stu;

import com.itcjy.emp.pojo.entity.SysClass;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "学员班级下拉选项")
public record StudentClassOptionRes(
        @Schema(description = "班级ID") Long id,
        @Schema(description = "班级名称") String className
) {
    public static StudentClassOptionRes from(SysClass sysClass) {
        return new StudentClassOptionRes(sysClass.getId(), sysClass.getClassPeriod());
    }
}
