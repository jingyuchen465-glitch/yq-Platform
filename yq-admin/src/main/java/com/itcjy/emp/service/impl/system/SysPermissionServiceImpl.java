package com.itcjy.emp.service.impl.system;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.config.init.PermissionControllerMetadataRegistry;
import com.itcjy.emp.config.init.PermissionControllerMetadataRegistry.PermissionControllerMetadata;
import com.itcjy.emp.mapper.system.SysPermissionMapper;
import com.itcjy.emp.pojo.entity.SysPermission;
import com.itcjy.emp.pojo.req.system.SysPermissionPageReq;
import com.itcjy.emp.pojo.res.system.SysPermissionGroupRes;
import com.itcjy.emp.pojo.res.system.SysPermissionTreeRes;
import com.itcjy.emp.service.system.ISysPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
        implements ISysPermissionService {

    private static final String UNCLASSIFIED_GROUP_CODE = "unclassified";
    private static final String UNCLASSIFIED_GROUP_NAME = "未归类权限";
    private static final String UNCLASSIFIED_GROUP_DESCRIPTION = "数据库中存在，但当前 Controller 中已找不到对应注解的权限";

    private final PermissionControllerMetadataRegistry metadataRegistry;

    @Override
    public List<SysPermission> listPermissions(SysPermissionPageReq req) {
        return this.list(buildQueryWrapper(req));
    }

    @Override
    public PageResult<SysPermission> pagePermissions(SysPermissionPageReq req) {
        IPage<SysPermission> page = this.page(new Page<>(req.getCurrent(), req.getSize()), buildQueryWrapper(req));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Override
    public SysPermissionTreeRes listPermissionTree(SysPermissionPageReq req) {
        Map<String, PermissionGroup> permissionGroups = new LinkedHashMap<>();
        listPermissions(req).forEach(permission -> {
            PermissionControllerMetadata metadata = metadataRegistry
                    .findByPermissionCode(permission.getPermissionCode())
                    .orElse(null);
            PermissionGroup group = metadata == null
                    ? permissionGroups.computeIfAbsent(UNCLASSIFIED_GROUP_CODE, key -> PermissionGroup.unclassified())
                    : permissionGroups.computeIfAbsent(metadata.controllerName(), key -> PermissionGroup.from(metadata));
            group.permissions().add(permission);
        });

        List<SysPermissionGroupRes> groups = permissionGroups.values().stream()
                .sorted((left, right) -> left.groupName().compareToIgnoreCase(right.groupName()))
                .map(group -> SysPermissionGroupRes.from(
                        group.groupCode(),
                        group.groupName(),
                        group.groupDescription(),
                        group.controllerName(),
                        group.permissions()
                ))
                .toList();
        return SysPermissionTreeRes.from(groups);
    }

    private LambdaQueryWrapper<SysPermission> buildQueryWrapper(SysPermissionPageReq req) {
        return Wrappers.<SysPermission>lambdaQuery()
                .like(StrUtil.isNotBlank(req.getPermissionCode()), SysPermission::getPermissionCode, req.getPermissionCode())
                .like(StrUtil.isNotBlank(req.getPermissionName()), SysPermission::getPermissionName, req.getPermissionName())
                .like(StrUtil.isNotBlank(req.getApiPath()), SysPermission::getApiPath, req.getApiPath())
                .eq(StrUtil.isNotBlank(req.getStatus()), SysPermission::getStatus, req.getStatus())
                .orderByAsc(SysPermission::getPermissionCode);
    }

    @Override
    public void updateStatus(Long id, String status) {
        SysPermission permission = this.getById(id);
        if (permission == null) {
            throw BusinessException.PERMISSION_NOT_EXIST.newInstance("权限不存在");
        }
        permission.setStatus(status);
        permission.setUpdatedAt(LocalDateTime.now());
        this.updateById(permission);
    }

    private record PermissionGroup(
            String groupCode,
            String groupName,
            String groupDescription,
            String controllerName,
            List<SysPermission> permissions
    ) {
        private static PermissionGroup from(PermissionControllerMetadata metadata) {
            return new PermissionGroup(
                    metadata.controllerName(),
                    metadata.groupName(),
                    metadata.groupDescription(),
                    metadata.controllerName(),
                    new ArrayList<>()
            );
        }

        private static PermissionGroup unclassified() {
            return new PermissionGroup(
                    UNCLASSIFIED_GROUP_CODE,
                    UNCLASSIFIED_GROUP_NAME,
                    UNCLASSIFIED_GROUP_DESCRIPTION,
                    "-",
                    new ArrayList<>()
            );
        }
    }
}
