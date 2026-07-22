package com.itcjy.emp.controller.system;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.pojo.req.system.RolePermissionReq;
import com.itcjy.emp.service.system.ISysRolePermissionService;
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
@RequestMapping("/emp/sysRole")
@Tag(name = "角色授权", description = "角色权限分配相关接口")
public class SysRolePermissionController {

    @Resource
    private ISysRolePermissionService sysRolePermissionService;

    @Operation(summary = "角色授权", description = "全量覆盖指定角色拥有的权限")
    @PutMapping("/{roleId}/permissions")
    @HasPermission(code = "sys:role:permission:update", name = "角色授权", description = "全量覆盖指定角色拥有的权限")
    public ApiResponse<Void> assignPermissions(@Parameter(description = "角色ID", required = true)
                                               @PathVariable @NotNull(message = "角色ID不能为空") Long roleId,
                                               @Valid @RequestBody RolePermissionReq req) {
        sysRolePermissionService.assignPermissions(roleId, req.getPermissionIds());
        return ApiResponse.success("授权成功");
    }

    @Operation(summary = "查询角色已拥有权限", description = "查询指定角色已拥有的权限ID列表")
    @GetMapping("/{roleId}/permissions")
    @HasPermission(code = "sys:role:permission:get", name = "查询角色权限", description = "查询指定角色已拥有的权限ID列表")
    public ApiResponse<List<Long>> listPermissionIds(@Parameter(description = "角色ID", required = true)
                                                     @PathVariable @NotNull(message = "角色ID不能为空") Long roleId) {
        return ApiResponse.success(sysRolePermissionService.listPermissionIds(roleId));
    }
}