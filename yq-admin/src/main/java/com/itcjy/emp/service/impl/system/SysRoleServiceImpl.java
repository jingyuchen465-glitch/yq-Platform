package com.itcjy.emp.service.impl.system;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.system.SysRoleMapper;
import com.itcjy.emp.pojo.entity.SysRole;
import com.itcjy.emp.pojo.entity.SysRolePermission;
import com.itcjy.emp.pojo.entity.SysUserRole;
import com.itcjy.emp.pojo.req.system.SysRolePageReq;
import com.itcjy.emp.pojo.req.system.SysRoleReq;
import com.itcjy.emp.pojo.res.system.SysRoleOptionRes;
import com.itcjy.emp.service.system.ISysRolePermissionService;
import com.itcjy.emp.service.system.ISysRoleService;
import com.itcjy.emp.service.system.ISysUserRoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
        SysRole role = this.getById(id);
        if (role == null) {
            throw BusinessException.ROLE_NOT_EXIST.newInstance("角色不存在");
        }
        if ("ACTIVE".equals(role.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("启用状态的角色不能删除，请先禁用");
        }
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
                        .orderByAsc(SysRole::getId)
        );
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Override
    public List<SysRoleOptionRes> listRoleOptions() {
        return this.lambdaQuery()
                .orderByAsc(SysRole::getId)
                .list()
                .stream()
                .map(SysRoleOptionRes::from)
                .collect(Collectors.toList());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoles(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.PARAMS_ERROR.newInstance("删除ID列表不能为空");
        }
        List<SysRole> roles = this.listByIds(ids);
        if (roles.size() != ids.size()) {
            throw BusinessException.ROLE_NOT_EXIST.newInstance("部分角色不存在");
        }
        // 检查是否有启用状态的角色
        List<String> activeRoleNames = roles.stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .map(SysRole::getRoleName)
                .collect(Collectors.toList());
        if (!activeRoleNames.isEmpty()) {
            throw BusinessException.DATA_ERROR.newInstance(
                    "角色「" + String.join("、", activeRoleNames) + "」处于启用状态，不能删除，请先禁用");
        }
        // 检查是否有角色正在被用户使用
        List<SysUserRole> usedRelations = sysUserRoleService.lambdaQuery()
                .in(SysUserRole::getRoleId, ids)
                .list();
        if (!usedRelations.isEmpty()) {
            List<Long> usedRoleIds = usedRelations.stream()
                    .map(SysUserRole::getRoleId).distinct().collect(Collectors.toList());
            List<String> usedRoleNames = this.listByIds(usedRoleIds).stream()
                    .map(SysRole::getRoleName).collect(Collectors.toList());
            throw BusinessException.DATA_ERROR.newInstance(
                    "角色「" + String.join("、", usedRoleNames) + "」正在被用户使用，不能删除");
        }
        sysRolePermissionService.remove(Wrappers.<SysRolePermission>lambdaQuery()
                .in(SysRolePermission::getRoleId, ids));
        this.removeByIds(ids);
    }
}
