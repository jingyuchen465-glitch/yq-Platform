package com.itcjy.emp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.emp.pojo.entity.SysRolePermission;

import java.util.List;

public interface ISysRolePermissionService extends IService<SysRolePermission> {

    /**
     * 全量覆盖角色拥有的权限。
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 查询角色已拥有的权限ID列表。
     */
    List<Long> listPermissionIds(Long roleId);
}