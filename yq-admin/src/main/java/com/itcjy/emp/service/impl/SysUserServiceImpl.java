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
import com.itcjy.emp.pojo.entity.SysRole;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.entity.SysUserRole;
import com.itcjy.emp.pojo.req.SysUserPageReq;
import com.itcjy.emp.pojo.req.SysUserReq;
import com.itcjy.emp.pojo.req.SysUserUpdateReq;
import com.itcjy.emp.pojo.res.SysUserRes;
import com.itcjy.emp.service.ISysRoleService;
import com.itcjy.emp.service.ISysUserRoleService;
import com.itcjy.emp.service.ISysUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Resource
    private ISysUserRoleService sysUserRoleService;

    @Resource
    private ISysRoleService sysRoleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(SysUserReq req) {
        SysUser sysUser = BeanUtil.copyProperties(req, SysUser.class);
        this.save(sysUser);
        sysUserRoleService.assignDefaultRole(sysUser.getId());
    }

    @Override
    public boolean existsById(Long id) {
        return this.lambdaQuery().eq(SysUser::getId, id).exists();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw BusinessException.USER_NOT_EXIST.newInstance("用户不存在");
        }
        if ("ACTIVE".equals(user.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("启用状态的用户不能删除，请先禁用");
        }
        sysUserRoleService.remove(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, id));
        this.removeById(id);
    }

    @Override
    public PageResult<SysUserRes> pageUsers(SysUserPageReq req) {
        IPage<SysUser> page = this.page(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<SysUser>lambdaQuery()
                        .like(StrUtil.isNotBlank(req.getUsername()), SysUser::getUsername, req.getUsername())
                        .like(StrUtil.isNotBlank(req.getNickname()), SysUser::getNickname, req.getNickname())
                        .like(StrUtil.isNotBlank(req.getRealName()), SysUser::getRealName, req.getRealName())
                        .orderByAsc(SysUser::getId)
        );
        List<SysUser> users = page.getRecords();
        if (users.isEmpty()) {
            return new PageResult<>(page.getTotal(), Collections.emptyList());
        }

        // 批量查询当前页用户的角色关系，再批量查角色，避免 N+1
        List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
        List<SysUserRole> userRoles = sysUserRoleService.list(Wrappers.<SysUserRole>lambdaQuery()
                .in(SysUserRole::getUserId, userIds));

        Map<Long, SysRole> roleMap = Collections.emptyMap();
        if (!userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).distinct().collect(Collectors.toList());
            roleMap = sysRoleService.listByIds(roleIds).stream()
                    .collect(Collectors.toMap(SysRole::getId, Function.identity()));
        }

        // 按 userId 分组角色列表
        Map<Long, SysRole> finalRoleMap = roleMap;
        Map<Long, List<SysRole>> userRoleMap = userRoles.stream()
                .map(ur -> {
                    SysRole role = finalRoleMap.get(ur.getRoleId());
                    return role != null ? Map.entry(ur.getUserId(), role) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        List<SysUserRes> records = users.stream().map(user -> {
            SysUserRes res = BeanUtil.copyProperties(user, SysUserRes.class);
            res.setPassword(null);
            res.setRoles(userRoleMap.getOrDefault(user.getId(), Collections.emptyList()));
            return res;
        }).collect(Collectors.toList());

        return new PageResult<>(page.getTotal(), records);
    }

    @Override
    public void updateUser(Long id, SysUserUpdateReq req) {
        if (!existsById(id)) {
            throw BusinessException.USER_NOT_EXIST.newInstance("用户不存在");
        }
        SysUser sysUser = BeanUtil.copyProperties(req, SysUser.class);
        sysUser.setId(id);
        this.updateById(sysUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.PARAMS_ERROR.newInstance("删除ID列表不能为空");
        }
        List<SysUser> users = this.listByIds(ids);
        if (users.size() != ids.size()) {
            throw BusinessException.USER_NOT_EXIST.newInstance("部分用户不存在");
        }
        List<String> activeNames = users.stream()
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                .collect(Collectors.toList());
        if (!activeNames.isEmpty()) {
            throw BusinessException.DATA_ERROR.newInstance(
                    "用户「" + String.join("、", activeNames) + "」处于启用状态，不能删除，请先禁用");
        }
        sysUserRoleService.remove(Wrappers.<SysUserRole>lambdaQuery()
                .in(SysUserRole::getUserId, ids));
        this.removeByIds(ids);
    }
}