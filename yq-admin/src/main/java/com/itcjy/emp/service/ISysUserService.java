package com.itcjy.emp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.req.SysUserPageReq;
import com.itcjy.emp.pojo.req.SysUserReq;

public interface ISysUserService extends IService<SysUser> {

    /**
     * 新增用户，并默认分配讲师角色。
     */
    void addUser(SysUserReq req);

    /**
     * 判断指定 ID 的用户是否存在
     */
    boolean existsById(Long id);

    /**
     * 删除用户，并清理用户角色关系。
     */
    void deleteUser(Long id);

    /**
     * 分页查询用户
     */
    PageResult<SysUser> pageUsers(SysUserPageReq req);

    /**
     * 根据 ID 更新用户信息
     */
    void updateUser(Long id, SysUserReq req);
}