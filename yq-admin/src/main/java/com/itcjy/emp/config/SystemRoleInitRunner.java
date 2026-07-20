package com.itcjy.emp.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.emp.mapper.SysRoleMapper;
import com.itcjy.emp.pojo.entity.SysRole;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Order(0)
@ConditionalOnProperty(prefix = "app.role-init", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SystemRoleInitRunner implements ApplicationRunner {

    private static final List<SystemRole> SYSTEM_ROLES = List.of(
            new SystemRole("ADMIN", "超级管理员", "拥有系统全部权限"),
            new SystemRole("LECTURER", "讲师", "负责课程教学与学员管理"),
            new SystemRole("STUDENT", "学生", "系统学员角色"),
            new SystemRole("OPERATOR", "运营人员", "负责后台运营管理")
    );

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        SYSTEM_ROLES.forEach(this::insertIfAbsent);
    }

    private void insertIfAbsent(SystemRole systemRole) {
        boolean exists = sysRoleMapper.selectCount(
                Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getRoleCode, systemRole.getRoleCode())
        ) > 0;
        if (exists) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        SysRole role = new SysRole();
        role.setRoleCode(systemRole.getRoleCode());
        role.setRoleName(systemRole.getRoleName());
        role.setDescription(systemRole.getDescription());
        role.setStatus(ActiveEnum.ACTIVE.name());
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        sysRoleMapper.insert(role);
        log.info("初始化系统角色: code={}, name={}", systemRole.getRoleCode(), systemRole.getRoleName());
    }

    @Getter
    @AllArgsConstructor
    private static class SystemRole {
        private String roleCode;
        private String roleName;
        private String description;
    }
}