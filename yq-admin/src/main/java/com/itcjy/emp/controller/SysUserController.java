package com.itcjy.emp.controller;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.constants.TokenConstants;
import com.itcjy.emp.pojo.req.LoginReq;
import com.itcjy.emp.pojo.res.LoginRes;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.req.SysUserPageReq;
import com.itcjy.emp.pojo.req.SysUserReq;
import com.itcjy.emp.pojo.req.SysUserUpdateReq;
import com.itcjy.emp.pojo.res.SysUserRes;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequestMapping("/emp/sysUser")
@Tag(name = "员工管理", description = "员工用户相关接口")
public class SysUserController {

    @Resource
    private ISysUserService sysUserService;

    @Operation(summary = "员工登录", description = "根据用户名和密码登录，返回token")
    @PostMapping("/login")
    public ApiResponse<LoginRes> empLogin(@Valid @RequestBody LoginReq req) {
        return ApiResponse.success(sysUserService.empLogin(req));
    }

    @Operation(summary = "员工退出登录", description = "清理当前登录用户在Redis中的登录态")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(TokenConstants.AUTHORIZATION) String authorization) {
        sysUserService.logout(authorization);
        return ApiResponse.success("退出成功");
    }

    @Operation(summary = "添加员工", description = "新增员工用户信息，默认分配讲师角色")
    @PostMapping("/add")
    @HasPermission(code = "sys:user:add", name = "添加用户", description = "新增系统用户")
    public ApiResponse<Void> addSysUser(@Valid @RequestBody SysUserReq req) {
        sysUserService.addUser(req);
        return ApiResponse.success();
    }

    @Operation(summary = "删除员工", description = "根据ID删除员工，并清理用户角色关系")
    @DeleteMapping("/delete/{id}")
    @HasPermission(code = "sys:user:delete", name = "删除用户", description = "根据ID删除系统用户")
    public ApiResponse<Void> deleteSysUser(@Parameter(description = "用户ID", required = true)
                                           @PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        sysUserService.deleteUser(id);
        return ApiResponse.success("删除成功");
    }

    @Operation(summary = "修改员工", description = "根据ID修改员工信息")
    @PutMapping("/update/{id}")
    @HasPermission(code = "sys:user:update", name = "修改用户", description = "根据ID修改系统用户")
    public ApiResponse<Void> updateSysUser(@Parameter(description = "用户ID", required = true)
                                           @PathVariable @NotNull(message = "用户ID不能为空") Long id,
                                           @Valid @RequestBody SysUserUpdateReq req) {
        sysUserService.updateUser(id, req);
        return ApiResponse.success("修改成功");
    }

    @Operation(summary = "查询员工详情", description = "根据ID查询员工详情")
    @GetMapping("/get/{id}")
    @HasPermission(code = "sys:user:get", name = "查询用户详情", description = "根据ID查询系统用户详情")
    public ApiResponse<SysUser> getSysUser(@Parameter(description = "用户ID", required = true)
                                           @PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        SysUser sysUser = sysUserService.getById(id);
        if (sysUser == null) {
            throw BusinessException.USER_NOT_EXIST.newInstance("用户不存在");
        }
        return ApiResponse.success(sysUser);
    }

    @Operation(summary = "分页查询员工", description = "支持 username、nickname、real_name 模糊查询")
    @GetMapping("/page")
    @HasPermission(code = "sys:user:page", name = "分页查询用户", description = "分页查询系统用户")
    public ApiResponse<PageResult<SysUserRes>> pageSysUser(@Valid @ParameterObject SysUserPageReq req) {
        return ApiResponse.success(sysUserService.pageUsers(req));
    }

    @Operation(summary = "批量删除员工", description = "根据ID列表批量删除员工，并清理用户角色关系")
    @DeleteMapping("/batchDelete")
    @HasPermission(code = "sys:user:batchDelete", name = "批量删除用户", description = "批量删除系统用户")
    public ApiResponse<Void> batchDeleteSysUser(@RequestBody List<@NotNull(message = "用户ID不能为空") Long> ids) {
        sysUserService.deleteUsers(ids);
        return ApiResponse.success("批量删除成功");
    }
}
