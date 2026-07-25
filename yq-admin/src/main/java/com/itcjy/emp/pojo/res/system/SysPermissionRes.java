package com.itcjy.emp.pojo.res.system;

import com.itcjy.emp.pojo.entity.SysPermission;

import java.time.LocalDateTime;

public record SysPermissionRes(
        Long id,
        String permissionCode,
        String permissionName,
        String apiPath,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SysPermissionRes from(SysPermission permission) {
        return new SysPermissionRes(
                permission.getId(),
                permission.getPermissionCode(),
                permission.getPermissionName(),
                permission.getApiPath(),
                permission.getDescription(),
                permission.getStatus(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }
}
