package com.itcjy.emp.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.emp.pojo.entity.SysUserRole;

import java.util.List;

public interface ISysUserRoleService extends IService<SysUserRole> {

    /**
     * 全量覆盖用户拥有的角色。
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 查询用户已拥有的角色ID列表。
     */
    List<Long> listRoleIds(Long userId);

    /**
     * 给用户分配默认角色。
     */
    void assignDefaultRole(Long userId);
}