package com.itcjy.emp.pojo.req.system;

import com.itcjy.common.annotations.StatusEnum;
import com.itcjy.common.myEnum.ActiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "角色请求参数")
public class SysRoleReq {

    @Schema(description = "角色编码", example = "LECTURER")
    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$", message = "角色编码必须为2到64位大写字母、数字或下划线，且以大写字母开头")
    private String roleCode;

    @Schema(description = "角色名称", example = "讲师")
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过64个字符")
    private String roleName;

    @Schema(description = "角色描述", example = "负责课程教学与学员管理")
    @Size(max = 255, message = "角色描述长度不能超过255个字符")
    private String description;

    @Schema(description = "状态", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    @NotBlank(message = "状态不能为空")
    @StatusEnum(enumClass = ActiveEnum.class, message = "状态值不正确")
    private String status;
}