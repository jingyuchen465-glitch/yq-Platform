package com.itcjy.emp.controller;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysRole;
import com.itcjy.emp.pojo.req.SysRolePageReq;
import com.itcjy.emp.pojo.req.SysRoleReq;
import com.itcjy.emp.pojo.res.SysRoleOptionRes;
import com.itcjy.emp.service.ISysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/emp/sysRole")
@Tag(name = "角色管理", description = "系统角色维护接口")
public class SysRoleController {

    @Resource
    private ISysRoleService sysRoleService;

    @Operation(summary = "新增角色", description = "新增系统角色")
    @PostMapping("/add")
    @HasPermission(code = "sys:role:add", name = "新增角色", description = "新增系统角色")
    public ApiResponse<Void> addRole(@Valid @RequestBody SysRoleReq req) {
        sysRoleService.addRole(req);
        return ApiResponse.success();
    }

    @Operation(summary = "删除角色", description = "根据ID删除角色，角色正在被用户使用时不允许删除")
    @DeleteMapping("/delete/{id}")
    @HasPermission(code = "sys:role:delete", name = "删除角色", description = "根据ID删除系统角色")
    public ApiResponse<Void> deleteRole(@Parameter(description = "角色ID", required = true)
                                        @PathVariable @NotNull(message = "角色ID不能为空") Long id) {
        sysRoleService.deleteRole(id);
        return ApiResponse.success("删除成功");
    }

    @Operation(summary = "修改角色", description = "根据ID修改角色")
    @PutMapping("/update/{id}")
    @HasPermission(code = "sys:role:update", name = "修改角色", description = "根据ID修改系统角色")
    public ApiResponse<Void> updateRole(@Parameter(description = "角色ID", required = true)
                                        @PathVariable @NotNull(message = "角色ID不能为空") Long id,
                                        @Valid @RequestBody SysRoleReq req) {
        sysRoleService.updateRole(id, req);
        return ApiResponse.success("修改成功");
    }

    @Operation(summary = "查询角色详情", description = "根据ID查询角色详情")
    @GetMapping("/get/{id}")
    @HasPermission(code = "sys:role:get", name = "查询角色详情", description = "根据ID查询系统角色详情")
    public ApiResponse<SysRole> getRole(@Parameter(description = "角色ID", required = true)
                                        @PathVariable @NotNull(message = "角色ID不能为空") Long id) {
        SysRole role = sysRoleService.getById(id);
        if (role == null) {
            throw BusinessException.ROLE_NOT_EXIST.newInstance("角色不存在");
        }
        return ApiResponse.success(role);
    }

    @Operation(summary = "分页查询角色", description = "支持 roleCode、roleName 模糊查询和 status 精确查询")
    @GetMapping("/page")
    @HasPermission(code = "sys:role:page", name = "分页查询角色", description = "分页查询系统角色")
    public ApiResponse<PageResult<SysRole>> pageRoles(@Valid @ParameterObject SysRolePageReq req) {
        return ApiResponse.success(sysRoleService.pageRoles(req));
    }

    @Operation(summary = "查询角色下拉选项", description = "查询数据库现存角色，用于用户管理页面角色筛选")
    @GetMapping("/list")
    @HasPermission(code = "sys:role:list", name = "查询角色列表", description = "查询角色下拉选项")
    public ApiResponse<List<SysRoleOptionRes>> listRoleOptions() {
        return ApiResponse.success(sysRoleService.listRoleOptions());
    }

    @Operation(summary = "批量删除角色", description = "根据ID列表批量删除角色，角色正在被用户使用时不允许删除")
    @DeleteMapping("/batchDelete")
    @HasPermission(code = "sys:role:batchDelete", name = "批量删除角色", description = "批量删除系统角色")
    public ApiResponse<Void> batchDeleteRole(@RequestBody List<@NotNull(message = "角色ID不能为空") Long> ids) {
        sysRoleService.deleteRoles(ids);
        return ApiResponse.success("批量删除成功");
    }
}
