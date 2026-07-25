package com.itcjy.emp.config.init;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.emp.config.init.PermissionControllerMetadataRegistry.PermissionControllerMetadata;
import com.itcjy.emp.mapper.system.SysPermissionMapper;
import com.itcjy.emp.pojo.entity.SysPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 权限同步Runner
 * <p>
 * 应用启动后自动扫描所有标注了 {@link HasPermission} 注解的接口方法，
 * 将权限信息同步到 sys_permission 表：数据库中不存在的权限则新增，已存在的则更新。
 * 可通过配置 app.permission-sync.enabled=false 关闭该功能。
 * </p>
 */
@Slf4j
@Component
@Order(1)//就是排队拿号，数字越小越靠前执行。
@ConditionalOnProperty(prefix = "app.permission-sync", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class PermissionSyncRunner implements ApplicationRunner {

    private final PermissionControllerMetadataRegistry metadataRegistry;
    private final SysPermissionMapper sysPermissionMapper;

    /**
     * 应用启动完成后执行：遍历所有接口方法，逐个同步权限
     *
     * @param args 启动参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        metadataRegistry.listAll().forEach(this::syncPermission);
    }

    /**
     * 同步单个接口的权限信息
     * <p>未标注 {@link HasPermission} 注解的接口直接跳过；根据权限编码判断是新增还是更新。</p>
     *
     * @param metadata Controller 中解析出的权限元数据
     */
    private void syncPermission(PermissionControllerMetadata metadata) {
        String permissionCode = metadata.permissionCode();
        // 根据权限编码查询数据库中是否已存在该权限
        SysPermission dbPermission = sysPermissionMapper.selectOne(
                Wrappers.<SysPermission>lambdaQuery()
                        .eq(SysPermission::getPermissionCode, permissionCode)
        );

        // 不存在则新增，存在则更新
        if (dbPermission == null) {
            insertPermission(metadata);
            return;
        }

        updatePermission(dbPermission, metadata);
    }

    /**
     * 新增权限记录
     *
     * @param metadata Controller 中解析出的权限元数据
     */
    private void insertPermission(PermissionControllerMetadata metadata) {
        LocalDateTime now = LocalDateTime.now();
        SysPermission permission = new SysPermission();
        permission.setPermissionCode(metadata.permissionCode());
        permission.setPermissionName(metadata.permissionName());
        permission.setApiPath(metadata.apiPath());
        permission.setDescription(metadata.permissionDescription());
        permission.setStatus(ActiveEnum.ACTIVE.name());
        permission.setCreatedAt(now);
        permission.setUpdatedAt(now);
        sysPermissionMapper.insert(permission);
        log.info("同步新增权限: code={}, path={}", metadata.permissionCode(), metadata.apiPath());
    }

    /**
     * 更新已存在的权限记录
     *
     * @param permission    数据库中已存在的权限实体
     * @param metadata   Controller 中解析出的权限元数据
     */
    private void updatePermission(SysPermission permission, PermissionControllerMetadata metadata) {
        permission.setPermissionName(metadata.permissionName());
        permission.setApiPath(metadata.apiPath());
        permission.setDescription(metadata.permissionDescription());
        permission.setUpdatedAt(LocalDateTime.now());
        sysPermissionMapper.updateById(permission);
        log.info("同步更新权限: code={}, path={}", metadata.permissionCode(), metadata.apiPath());
    }
}
