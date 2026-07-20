package com.itcjy.emp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysPermission;
import com.itcjy.emp.pojo.req.SysPermissionPageReq;

import java.util.List;

public interface ISysPermissionService extends IService<SysPermission> {

    /**
     * 查询全部权限。
     */
    List<SysPermission> listPermissions(SysPermissionPageReq req);

    /**
     * 分页查询权限。
     */
    PageResult<SysPermission> pagePermissions(SysPermissionPageReq req);
}