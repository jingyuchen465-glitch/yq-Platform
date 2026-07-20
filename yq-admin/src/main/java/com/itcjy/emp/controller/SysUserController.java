package com.itcjy.emp.controller;

import cn.hutool.core.bean.BeanUtil;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.req.SysUserReq;
import com.itcjy.emp.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/emp/sysUser")
@Tag(name = "员工管理", description = "员工用户相关接口")
public class SysUserController {

    @Resource
    private ISysUserService sysUserService;

    @Operation(summary = "添加员工", description = "新增员工用户信息")
    @RequestMapping("/add")
    public ApiResponse<Void> addSysUser(@Valid @RequestBody SysUserReq req) {
        SysUser sysUser = BeanUtil.copyProperties(req, SysUser.class);
        sysUserService.save(sysUser);
        return ApiResponse.success();
    }
}
