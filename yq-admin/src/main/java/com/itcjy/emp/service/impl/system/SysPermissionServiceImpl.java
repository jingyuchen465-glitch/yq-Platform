package com.itcjy.emp.service.impl.system;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.system.SysPermissionMapper;
import com.itcjy.emp.pojo.entity.SysPermission;
import com.itcjy.emp.pojo.req.system.SysPermissionPageReq;
import com.itcjy.emp.service.system.ISysPermissionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
        implements ISysPermissionService {

    @Override
    public List<SysPermission> listPermissions(SysPermissionPageReq req) {
        return this.list(buildQueryWrapper(req));
    }

    @Override
    public PageResult<SysPermission> pagePermissions(SysPermissionPageReq req) {
        IPage<SysPermission> page = this.page(new Page<>(req.getCurrent(), req.getSize()), buildQueryWrapper(req));
        return new PageResult<>(page.getTotal(), page.getRecords());
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
}