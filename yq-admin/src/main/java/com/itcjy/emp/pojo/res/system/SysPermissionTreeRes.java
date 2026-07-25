package com.itcjy.emp.pojo.res.system;

import java.util.List;

public record SysPermissionTreeRes(
        int total,
        long activeTotal,
        int groupTotal,
        List<SysPermissionGroupRes> groups
) {
    public static SysPermissionTreeRes from(List<SysPermissionGroupRes> groups) {
        int total = groups.stream().mapToInt(SysPermissionGroupRes::total).sum();
        long activeTotal = groups.stream().mapToLong(SysPermissionGroupRes::activeTotal).sum();
        return new SysPermissionTreeRes(total, activeTotal, groups.size(), groups);
    }
}
