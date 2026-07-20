package com.itcjy.emp.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.emp.mapper.SysPermissionMapper;
import com.itcjy.emp.pojo.entity.SysPermission;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

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
public class PermissionSyncRunner implements ApplicationRunner {

    /**
     * Spring MVC 请求映射处理器，用于获取所有接口方法及其路由信息
     */
    @Resource
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * 权限表 Mapper，用于权限数据的增删改查
     */
    @Resource
    private SysPermissionMapper sysPermissionMapper;

    /**
     * 应用启动完成后执行：遍历所有接口方法，逐个同步权限
     *
     * @param args 启动参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        requestMappingHandlerMapping.getHandlerMethods().forEach(this::syncPermission);
    }

    /**
     * 同步单个接口的权限信息
     * <p>未标注 {@link HasPermission} 注解的接口直接跳过；根据权限编码判断是新增还是更新。</p>
     *
     * @param mappingInfo   接口的路由映射信息
     * @param handlerMethod 接口的处理方法
     */
    private void syncPermission(RequestMappingInfo mappingInfo, HandlerMethod handlerMethod) {
        // 获取方法上的权限注解，未标注则无需同步
        HasPermission hasPermission = handlerMethod.getMethodAnnotation(HasPermission.class);
        if (hasPermission == null) {
            return;
        }

        String permissionCode = hasPermission.code();
        String apiPath = resolveApiPath(mappingInfo);
        // 根据权限编码查询数据库中是否已存在该权限
        SysPermission dbPermission = sysPermissionMapper.selectOne(
                Wrappers.<SysPermission>lambdaQuery()
                        .eq(SysPermission::getPermissionCode, permissionCode)
        );

        // 不存在则新增，存在则更新
        if (dbPermission == null) {
            insertPermission(hasPermission, apiPath);
            return;
        }

        updatePermission(dbPermission, hasPermission, apiPath);
    }

    /**
     * 新增权限记录
     *
     * @param hasPermission 权限注解信息
     * @param apiPath       接口路径
     */
    private void insertPermission(HasPermission hasPermission, String apiPath) {
        LocalDateTime now = LocalDateTime.now();
        SysPermission permission = new SysPermission();
        permission.setPermissionCode(hasPermission.code());
        permission.setPermissionName(hasPermission.name());
        permission.setApiPath(apiPath);
        permission.setDescription(hasPermission.description());
        permission.setStatus(ActiveEnum.ACTIVE.name());
        permission.setCreatedAt(now);
        permission.setUpdatedAt(now);
        sysPermissionMapper.insert(permission);
        log.info("同步新增权限: code={}, path={}", hasPermission.code(), apiPath);
    }

    /**
     * 更新已存在的权限记录
     *
     * @param permission    数据库中已存在的权限实体
     * @param hasPermission 权限注解信息
     * @param apiPath       接口路径
     */
    private void updatePermission(SysPermission permission, HasPermission hasPermission, String apiPath) {
        permission.setPermissionName(hasPermission.name());
        permission.setApiPath(apiPath);
        permission.setDescription(hasPermission.description());
        permission.setUpdatedAt(LocalDateTime.now());
        sysPermissionMapper.updateById(permission);
        log.info("同步更新权限: code={}, path={}", hasPermission.code(), apiPath);
    }

    /**
     * 解析接口的访问路径
     * <p>一个接口可能配置了多个路径，按字典序排序后用逗号拼接。</p>
     *
     * @param mappingInfo 接口的路由映射信息
     * @return 接口路径字符串，无路径时返回空字符串
     */
    private String resolveApiPath(RequestMappingInfo mappingInfo) {
        Set<String> patternValues = mappingInfo.getPatternValues();
        if (CollectionUtils.isEmpty(patternValues)) {
            return "";
        }
        return patternValues.stream().sorted().collect(Collectors.joining(","));
    }
}