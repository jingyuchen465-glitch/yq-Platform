package com.itcjy.emp.pojo.req.system;

import com.itcjy.common.annotations.StatusEnum;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.common.myEnum.ConfigValueTypeEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SysConfigItemReq(
        @NotNull(message = "配置类型ID不能为空") Long typeId,
        @NotBlank(message = "配置项键不能为空")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$", message = "配置项键必须为2到64位大写字母、数字或下划线")
        String itemKey,
        @NotBlank(message = "配置项值不能为空")
        @Size(max = 1024, message = "配置项值不能超过1024个字符")
        String itemValue,
        @NotBlank(message = "配置值类型不能为空")
        @StatusEnum(enumClass = ConfigValueTypeEnum.class, message = "配置值类型不正确")
        String valueType,
        @Size(max = 255, message = "配置项描述不能超过255个字符") String description,
        @NotBlank(message = "状态不能为空")
        @StatusEnum(enumClass = ActiveEnum.class, message = "状态值不正确")
        String status,
        @NotNull(message = "排序号不能为空") @Min(value = 0, message = "排序号不能小于0") Integer sortOrder
) {
}
