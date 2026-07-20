package com.itcjy.emp.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itcjy.emp.mapper.SysPermissionMapper;
import com.itcjy.emp.mapper.SysRoleMapper;
import com.itcjy.emp.mapper.SysRolePermissionMapper;
import com.itcjy.emp.pojo.entity.SysPermission;
import com.itcjy.emp.pojo.entity.SysRole;
import com.itcjy.emp.pojo.entity.SysRolePermission;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Initializes full permissions for the administrator role after permission sync.
 */
@Slf4j
@Component
@Order(2)
@ConditionalOnProperty(prefix = "app.admin-permission-init", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminPermissionInitRunner implements ApplicationRunner {

    @Value("${app.admin-permission-init.role-code:ADMIN}")
    private String adminRoleCode;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        SysRole adminRole = sysRoleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getRoleCode, adminRoleCode)
        );
        if (adminRole == null) {
            log.warn("ADMIN permission initialization skipped, role does not exist: roleCode={}", adminRoleCode);
            return;
        }

        List<SysPermission> permissions = sysPermissionMapper.selectList(Wrappers.emptyWrapper());
        if (permissions.isEmpty()) {
            log.info("ADMIN permission initialization skipped, no permissions found: roleCode={}", adminRoleCode);
            return;
        }

        Set<Long> existingPermissionIds = sysRolePermissionMapper.selectList(
                        Wrappers.<SysRolePermission>lambdaQuery()
                                .eq(SysRolePermission::getRoleId, adminRole.getId())
                ).stream()
                .map(SysRolePermission::getPermissionId)
                .collect(Collectors.toSet());

        List<SysRolePermission> missingRolePermissions = permissions.stream()
                .filter(permission -> !existingPermissionIds.contains(permission.getId()))
                .map(permission -> buildRolePermission(adminRole.getId(), permission.getId()))
                .toList();

        if (missingRolePermissions.isEmpty()) {
            log.info("ADMIN already has all permissions: roleCode={}, total={}", adminRoleCode, permissions.size());
            return;
        }

        missingRolePermissions.forEach(sysRolePermissionMapper::insert);
        log.info("ADMIN permissions initialized: roleCode={}, added={}, total={}",
                adminRoleCode, missingRolePermissions.size(), permissions.size());
    }

    private SysRolePermission buildRolePermission(Long roleId, Long permissionId) {
        SysRolePermission rolePermission = new SysRolePermission();
        rolePermission.setRoleId(roleId);
        rolePermission.setPermissionId(permissionId);
        rolePermission.setCreatedAt(LocalDateTime.now());
        return rolePermission;
    }
}