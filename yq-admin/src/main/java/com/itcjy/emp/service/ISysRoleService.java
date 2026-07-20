package com.itcjy.emp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysRole;
import com.itcjy.emp.pojo.req.SysRolePageReq;
import com.itcjy.emp.pojo.req.SysRoleReq;

public interface ISysRoleService extends IService<SysRole> {

    /**
     * 新增角色。
     */
    void addRole(SysRoleReq req);

    /**
     * 根据 ID 修改角色。
     */
    void updateRole(Long id, SysRoleReq req);

    /**
     * 根据 ID 删除角色；角色正在被用户使用时不允许删除。
     */
    void deleteRole(Long id);

    /**
     * 分页查询角色。
     */
    PageResult<SysRole> pageRoles(SysRolePageReq req);
}