package com.itcjy.emp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.SysRoleMapper;
import com.itcjy.emp.pojo.entity.SysRole;
import com.itcjy.emp.pojo.entity.SysRolePermission;
import com.itcjy.emp.pojo.entity.SysUserRole;
import com.itcjy.emp.pojo.req.SysRolePageReq;
import com.itcjy.emp.pojo.req.SysRoleReq;
import com.itcjy.emp.service.ISysRolePermissionService;
import com.itcjy.emp.service.ISysRoleService;
import com.itcjy.emp.service.ISysUserRoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    @Resource
    private ISysUserRoleService sysUserRoleService;

    @Resource
    private ISysRolePermissionService sysRolePermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRole(SysRoleReq req) {
        checkRoleCodeUnique(null, req.getRoleCode());
        SysRole role = BeanUtil.copyProperties(req, SysRole.class);
        LocalDateTime now = LocalDateTime.now();
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        this.save(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, SysRoleReq req) {
        checkRoleExists(id);
        checkRoleCodeUnique(id, req.getRoleCode());
        SysRole role = BeanUtil.copyProperties(req, SysRole.class);
        role.setId(id);
        role.setUpdatedAt(LocalDateTime.now());
        this.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        checkRoleExists(id);
        boolean usedByUser = sysUserRoleService.lambdaQuery()
                .eq(SysUserRole::getRoleId, id)
                .exists();
        if (usedByUser) {
            throw BusinessException.DATA_ERROR.newInstance("该角色正在被用户使用，不能删除");
        }

        sysRolePermissionService.remove(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, id));
        this.removeById(id);
    }

    @Override
    public PageResult<SysRole> pageRoles(SysRolePageReq req) {
        IPage<SysRole> page = this.page(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<SysRole>lambdaQuery()
                        .like(StrUtil.isNotBlank(req.getRoleCode()), SysRole::getRoleCode, req.getRoleCode())
                        .like(StrUtil.isNotBlank(req.getRoleName()), SysRole::getRoleName, req.getRoleName())
                        .eq(StrUtil.isNotBlank(req.getStatus()), SysRole::getStatus, req.getStatus())
                        .orderByDesc(SysRole::getId)
        );
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    private void checkRoleExists(Long id) {
        if (this.getById(id) == null) {
            throw BusinessException.ROLE_NOT_EXIST.newInstance("角色不存在");
        }
    }

    private void checkRoleCodeUnique(Long id, String roleCode) {
        boolean exists = this.lambdaQuery()
                .eq(SysRole::getRoleCode, roleCode)
                .ne(id != null, SysRole::getId, id)
                .exists();
        if (exists) {
            throw BusinessException.ROLE_EXIST.newInstance("角色编码已存在");
        }
    }
}