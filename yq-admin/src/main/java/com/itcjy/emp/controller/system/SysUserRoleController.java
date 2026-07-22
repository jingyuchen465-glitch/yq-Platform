package com.itcjy.emp.controller.system;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.pojo.req.system.UserRoleReq;
import com.itcjy.emp.service.system.ISysUserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/emp/sysUser")
@Tag(name = "用户角色", description = "用户角色分配相关接口")
public class SysUserRoleController {

    @Resource
    private ISysUserRoleService sysUserRoleService;

    @Operation(summary = "用户分配角色", description = "全量覆盖指定用户拥有的角色")
    @PutMapping("/{userId}/roles")
    @HasPermission(code = "sys:user:role:update", name = "用户分配角色", description = "全量覆盖指定用户拥有的角色")
    public ApiResponse<Void> assignRoles(@Parameter(description = "用户ID", required = true)
                                         @PathVariable @NotNull(message = "用户ID不能为空") Long userId,
                                         @Valid @RequestBody UserRoleReq req) {
        sysUserRoleService.assignRoles(userId, req.getRoleIds());
        return ApiResponse.success("分配成功");
    }

    @Operation(summary = "查询用户已拥有角色", description = "查询指定用户已拥有的角色ID列表")
    @GetMapping("/{userId}/roles")
    @HasPermission(code = "sys:user:role:get", name = "查询用户角色", description = "查询指定用户已拥有的角色ID列表")
    public ApiResponse<List<Long>> listRoleIds(@Parameter(description = "用户ID", required = true)
                                               @PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        return ApiResponse.success(sysUserRoleService.listRoleIds(userId));
    }
}