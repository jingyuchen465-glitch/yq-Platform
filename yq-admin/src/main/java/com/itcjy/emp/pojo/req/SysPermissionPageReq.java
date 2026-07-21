package com.itcjy.emp.pojo.req;

import com.itcjy.common.annotations.StatusEnum;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.common.pojo.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "权限分页查询参数")
public class SysPermissionPageReq extends BasePageReq {

    @Schema(description = "权限编码模糊查询", example = "sys:user")
    @Size(max = 100, message = "权限编码长度不能超过100个字符")
    private String permissionCode;

    @Schema(description = "权限名称模糊查询", example = "用户")
    @Size(max = 100, message = "权限名称长度不能超过100个字符")
    private String permissionName;

    @Schema(description = "API路径模糊查询", example = "/emp/sysUser")
    @Size(max = 255, message = "API路径长度不能超过255个字符")
    private String apiPath;

    @Schema(description = "状态", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    @StatusEnum(enumClass = ActiveEnum.class, message = "状态值不正确")
    private String status;
}
