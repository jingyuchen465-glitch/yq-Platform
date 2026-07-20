package com.itcjy.emp.controller;

import cn.hutool.core.bean.BeanUtil;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.req.SysUserPageReq;
import com.itcjy.emp.pojo.req.SysUserReq;
import com.itcjy.emp.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@Validated
@RequestMapping("/emp/sysUser")
@Tag(name = "员工管理", description = "员工用户相关接口")
public class SysUserController {

    @Resource
    private ISysUserService sysUserService;

    @Operation(summary = "添加员工", description = "新增员工用户信息")
    @PostMapping("/add")
    public ApiResponse<Void> addSysUser(@Valid @RequestBody SysUserReq req) {
        SysUser sysUser = BeanUtil.copyProperties(req, SysUser.class);
        sysUserService.save(sysUser);
        return ApiResponse.success();
    }

    @Operation(summary = "删除员工", description = "根据ID删除员工")
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteSysUser(@Parameter(description = "用户ID", required = true)
                                           @PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        boolean removed = sysUserService.removeById(id);
        if (!removed) {
            throw BusinessException.USER_NOT_EXIST.newInstance("用户不存在");
        }
        return ApiResponse.success("删除成功");
    }

    @Operation(summary = "修改员工", description = "根据ID修改员工信息")
    @PutMapping("/update/{id}")
    public ApiResponse<Void> updateSysUser(@Parameter(description = "用户ID", required = true)
                                           @PathVariable @NotNull(message = "用户ID不能为空") Long id,
                                           @Valid @RequestBody SysUserReq req) {
        sysUserService.updateUser(id, req);
        return ApiResponse.success("修改成功");
    }

    @Operation(summary = "查询员工详情", description = "根据ID查询员工详情")
    @GetMapping("/get/{id}")
    public ApiResponse<SysUser> getSysUser(@Parameter(description = "用户ID", required = true)
                                           @PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        SysUser sysUser = sysUserService.getById(id);
        if (sysUser == null) {
            throw BusinessException.USER_NOT_EXIST.newInstance("用户不存在");
        }
        return ApiResponse.success(sysUser);
    }

    @Operation(summary = "分页查询员工", description = "支持username、nickname、real_name模糊查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<SysUser>> pageSysUser(@Valid @ParameterObject SysUserPageReq req) {
        return ApiResponse.success(sysUserService.pageUsers(req));
    }
}
