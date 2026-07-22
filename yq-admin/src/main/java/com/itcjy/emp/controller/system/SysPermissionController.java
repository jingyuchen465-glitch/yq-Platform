package com.itcjy.emp.controller.system;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysPermission;
import com.itcjy.emp.pojo.req.system.SysPermissionPageReq;
import com.itcjy.emp.service.system.ISysPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/emp/sysPermission")
@Tag(name = "权限查询", description = "系统权限查询接口")
public class SysPermissionController {

    @Resource
    private ISysPermissionService sysPermissionService;

    @Operation(summary = "查询权限列表", description = "查询全部权限，支持权限编码、权限名称、API路径和状态过滤")
    @GetMapping("/list")
    @HasPermission(code = "sys:permission:list", name = "查询权限列表", description = "查询全部系统权限")
    public ApiResponse<List<SysPermission>> listPermissions(@Valid @ParameterObject SysPermissionPageReq req) {
        return ApiResponse.success(sysPermissionService.listPermissions(req));
    }

    @Operation(summary = "分页查询权限", description = "分页查询权限，支持权限编码、权限名称、API路径和状态过滤")
    @GetMapping("/page")
    @HasPermission(code = "sys:permission:page", name = "分页查询权限", description = "分页查询系统权限")
    public ApiResponse<PageResult<SysPermission>> pagePermissions(@Valid @ParameterObject SysPermissionPageReq req) {
        return ApiResponse.success(sysPermissionService.pagePermissions(req));
    }

    @Operation(summary = "查询权限详情", description = "根据ID查询权限详情")
    @GetMapping("/get/{id}")
    @HasPermission(code = "sys:permission:get", name = "查询权限详情", description = "根据ID查询系统权限详情")
    public ApiResponse<SysPermission> getPermission(@Parameter(description = "权限ID", required = true)
                                                    @PathVariable @NotNull(message = "权限ID不能为空") Long id) {
        SysPermission permission = sysPermissionService.getById(id);
        if (permission == null) {
            throw BusinessException.PERMISSION_NOT_EXIST.newInstance("权限不存在");
        }
        return ApiResponse.success(permission);
    }

    @Operation(summary = "切换权限状态", description = "启用或禁用指定接口权限")
    @PutMapping("/updateStatus/{id}")
    @HasPermission(code = "sys:permission:updateStatus", name = "切换权限状态", description = "启用或禁用系统接口权限")
    public ApiResponse<Void> updatePermissionStatus(@Parameter(description = "权限ID", required = true)
                                                    @PathVariable @NotNull(message = "权限ID不能为空") Long id,
                                                    @Parameter(description = "状态：ACTIVE/INACTIVE", required = true)
                                                    @RequestParam @NotNull(message = "状态不能为空") String status) {
        sysPermissionService.updateStatus(id, status);
        return ApiResponse.success("状态更新成功");
    }
}