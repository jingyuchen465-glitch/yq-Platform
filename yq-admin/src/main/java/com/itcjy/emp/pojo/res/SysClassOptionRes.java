package com.itcjy.emp.pojo.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "班级表单下拉选项")
public record SysClassOptionRes(
        @Schema(description = "选项ID") Long id,
        @Schema(description = "选项显示名称") String label
) {
}
