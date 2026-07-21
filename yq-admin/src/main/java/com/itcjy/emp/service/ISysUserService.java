package com.itcjy.emp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.req.LoginReq;
import com.itcjy.emp.pojo.req.SysUserPageReq;
import com.itcjy.emp.pojo.req.SysUserReq;
import com.itcjy.emp.pojo.req.SysUserUpdateReq;
import com.itcjy.emp.pojo.res.LoginRes;
import com.itcjy.emp.pojo.res.SysUserRes;

import java.util.List;

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
     * 分页查询用户，携带每个用户的角色列表
     */
    PageResult<SysUserRes> pageUsers(SysUserPageReq req);

    /**
     * 根据 ID 更新用户信息
     */
    void updateUser(Long id, SysUserUpdateReq req);

    /**
     * 批量删除用户，并清理用户角色关系。
     */
    void deleteUsers(List<Long> ids);

    LoginRes empLogin(LoginReq req);

    /**
     * 退出登录，清理当前用户在 Redis 中的登录态。
     */
    void logout(String authorization);
}
