package com.itcjy.emp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.SysUserMapper;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.req.SysUserPageReq;
import com.itcjy.emp.pojo.req.SysUserReq;
import com.itcjy.emp.service.ISysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Override
    public boolean existsById(Long id) {
        return this.lambdaQuery().eq(SysUser::getId, id).exists();
    }

    @Override
    public PageResult<SysUser> pageUsers(SysUserPageReq req) {
        IPage<SysUser> page = this.page(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<SysUser>lambdaQuery()
                        .like(StrUtil.isNotBlank(req.getUsername()), SysUser::getUsername, req.getUsername())
                        .like(StrUtil.isNotBlank(req.getNickname()), SysUser::getNickname, req.getNickname())
                        .like(StrUtil.isNotBlank(req.getRealName()), SysUser::getRealName, req.getRealName())
                        .orderByDesc(SysUser::getId)
        );
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Override
    public void updateUser(Long id, SysUserReq req) {
        if (!existsById(id)) {
            throw BusinessException.USER_NOT_EXIST.newInstance("用户不存在");
        }
        SysUser sysUser = BeanUtil.copyProperties(req, SysUser.class);
        sysUser.setId(id);
        this.updateById(sysUser);
    }
}
