package com.itcjy.emp.pojo.res;

import com.itcjy.emp.pojo.entity.SysRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "角色下拉选项")
public record SysRoleOptionRes(
        @Schema(description = "角色ID", example = "1") Long id,
        @Schema(description = "角色编码", example = "ADMIN") String roleCode,
        @Schema(description = "角色名称", example = "超级管理员") String roleName,
        @Schema(description = "状态", example = "ACTIVE") String status
) {
    public static SysRoleOptionRes from(SysRole role) {
        return new SysRoleOptionRes(role.getId(), role.getRoleCode(), role.getRoleName(), role.getStatus());
    }
}
