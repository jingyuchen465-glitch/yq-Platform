package com.itcjy.emp.pojo.res.system;

import com.itcjy.emp.pojo.entity.SysPermission;

import java.util.List;

public record SysPermissionGroupRes(
        String groupCode,
        String groupName,
        String groupDescription,
        String controllerName,
        int total,
        long activeTotal,
        List<SysPermissionRes> permissions
) {
    public static SysPermissionGroupRes from(String groupCode,
                                             String groupName,
                                             String groupDescription,
                                             String controllerName,
                                             List<SysPermission> permissions) {
        List<SysPermissionRes> permissionResponses = permissions.stream()
                .map(SysPermissionRes::from)
                .toList();
        long activeTotal = permissions.stream()
                .filter(permission -> "ACTIVE".equals(permission.getStatus()))
                .count();
        return new SysPermissionGroupRes(
                groupCode,
                groupName,
                groupDescription,
                controllerName,
                permissionResponses.size(),
                activeTotal,
                permissionResponses
        );
    }
}
