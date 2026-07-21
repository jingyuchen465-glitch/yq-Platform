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
@Schema(description = "角色分页查询参数")
public class SysRolePageReq extends BasePageReq {

    @Schema(description = "角色编码模糊查询", example = "LECT")
    @Size(max = 64, message = "角色编码长度不能超过64个字符")
    private String roleCode;

    @Schema(description = "角色名称模糊查询", example = "讲师")
    @Size(max = 64, message = "角色名称长度不能超过64个字符")
    private String roleName;

    @Schema(description = "状态", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    @StatusEnum(enumClass = ActiveEnum.class, message = "状态值不正确")
    private String status;
}
