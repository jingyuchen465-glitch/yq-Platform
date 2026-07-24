package com.itcjy.emp.pojo.req.system;

import com.itcjy.common.annotations.StatusEnum;
import com.itcjy.common.myEnum.ActiveEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SysConfigTypeReq(
        @NotBlank(message = "配置类型编码不能为空")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$", message = "配置类型编码必须为2到64位大写字母、数字或下划线")
        String typeCode,
        @NotBlank(message = "配置类型名称不能为空")
        @Size(max = 64, message = "配置类型名称不能超过64个字符")
        String typeName,
        @Size(max = 255, message = "配置类型描述不能超过255个字符")
        String description,
        @NotBlank(message = "状态不能为空")
        @StatusEnum(enumClass = ActiveEnum.class, message = "状态值不正确")
        String status
) {
}
