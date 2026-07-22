package com.itcjy.emp.service.impl.system;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.emp.mapper.system.SysRoleMapper;
import com.itcjy.emp.mapper.system.SysUserMapper;
import com.itcjy.emp.mapper.system.SysUserRoleMapper;
import com.itcjy.emp.pojo.entity.SysRole;
import com.itcjy.emp.pojo.entity.SysUserRole;
import com.itcjy.emp.service.system.ISysUserRoleService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole>
        implements ISysUserRoleService {

    @Value("${app.user.default-role-code:LECTURER}")
    private String defaultRoleCode;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        checkUserExists(userId);
        List<Long> distinctRoleIds = distinctRoleIds(roleIds);
        checkRolesExist(distinctRoleIds);

        this.remove(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, userId));

        if (distinctRoleIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<SysUserRole> userRoles = distinctRoleIds.stream()
                .map(roleId -> buildUserRole(userId, roleId, now))
                .toList();
        this.saveBatch(userRoles);
    }

    @Override
    public List<Long> listRoleIds(Long userId) {
        checkUserExists(userId);
        return this.lambdaQuery()
                .eq(SysUserRole::getUserId, userId)
                .orderByAsc(SysUserRole::getRoleId)
                .list()
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDefaultRole(Long userId) {
        String roleCode = defaultRoleCode == null ? "" : defaultRoleCode.trim();
        if (roleCode.isBlank()) {
            throw BusinessException.PARAMS_ERROR.newInstance("默认角色编码不能为空");
        }

        SysRole defaultRole = sysRoleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getRoleCode, roleCode)
        );
        if (defaultRole == null) {
            throw BusinessException.ROLE_NOT_EXIST.newInstance("默认角色不存在，请检查 app.user.default-role-code 配置");
        }
        assignRoles(userId, List.of(defaultRole.getId()));
    }

    private void checkUserExists(Long userId) {
        if (sysUserMapper.selectById(userId) == null) {
            throw BusinessException.USER_NOT_EXIST.newInstance("用户不存在");
        }
    }

    private List<Long> distinctRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.stream().anyMatch(Objects::isNull)) {
            throw BusinessException.PARAMS_ERROR.newInstance("角色ID列表不能为空");
        }
        return roleIds.stream().distinct().toList();
    }

    private void checkRolesExist(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw BusinessException.ROLE_NOT_EXIST.newInstance("存在无效的角色ID");
        }
    }

    private SysUserRole buildUserRole(Long userId, Long roleId, LocalDateTime now) {
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRole.setCreatedAt(now);
        return userRole;
    }
}