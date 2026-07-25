package com.itcjy.emp.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysPermission;
import com.itcjy.emp.pojo.req.system.SysPermissionPageReq;
import com.itcjy.emp.pojo.res.system.SysPermissionTreeRes;

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

    /**
     * 按 Controller 分类查询权限树。
     */
    SysPermissionTreeRes listPermissionTree(SysPermissionPageReq req);

    /**
     * 更新权限状态（启用/禁用）
     */
    void updateStatus(Long id, String status);
}
