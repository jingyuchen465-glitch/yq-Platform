package com.itcjy.emp.service.impl.system;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.emp.mapper.system.SysPermissionMapper;
import com.itcjy.emp.mapper.system.SysRoleMapper;
import com.itcjy.emp.mapper.system.SysRolePermissionMapper;
import com.itcjy.emp.pojo.entity.SysPermission;
import com.itcjy.emp.pojo.entity.SysRolePermission;
import com.itcjy.emp.service.system.ISysRolePermissionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 角色-权限关联服务实现类
 * <p>负责处理角色与权限之间的分配、查询等关联关系。</p>
 */
@Service
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, SysRolePermission>
        implements ISysRolePermissionService {

    /**
     * 角色表 Mapper，用于校验角色是否存在
     */
    @Resource
    private SysRoleMapper sysRoleMapper;

    /**
     * 权限表 Mapper，用于校验权限是否存在
     */
    @Resource
    private SysPermissionMapper sysPermissionMapper;

    /**
     * 为角色分配权限
     * <p>采用“先全部删除、再批量新增”的策略：先校验角色和权限的有效性，
     * 清除该角色原有的所有权限关联，再根据传入的权限ID列表重新建立关联。</p>
     *
     * @param roleId        角色ID
     * @param permissionIds 需要分配的权限ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        checkRoleExists(roleId);
        List<Long> distinctPermissionIds = distinctPermissionIds(permissionIds);
        checkPermissionsExist(distinctPermissionIds);

        // 先删除该角色原有的所有权限关联
        this.remove(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, roleId));

        // 权限列表为空表示清空权限，无需新增
        if (distinctPermissionIds.isEmpty()) {
            return;
        }

        // 批量构建并保存新的角色-权限关联记录
        LocalDateTime now = LocalDateTime.now();
        List<SysRolePermission> rolePermissions = distinctPermissionIds.stream()
                .map(permissionId -> buildRolePermission(roleId, permissionId, now))
                .toList();
        this.saveBatch(rolePermissions);
    }

    /**
     * 查询指定角色已分配的权限ID列表
     *
     * @param roleId 角色ID
     * @return 该角色关联的权限ID列表（按权限ID升序）
     */
    @Override
    public List<Long> listPermissionIds(Long roleId) {
        checkRoleExists(roleId);
        return this.lambdaQuery()
                .eq(SysRolePermission::getRoleId, roleId)
                .orderByAsc(SysRolePermission::getPermissionId)
                .list()
                .stream()
                .map(SysRolePermission::getPermissionId)
                .toList();
    }

    /**
     * 校验角色是否存在，不存在则抛出业务异常
     *
     * @param roleId 角色ID
     */
    private void checkRoleExists(Long roleId) {
        if (sysRoleMapper.selectById(roleId) == null) {
            throw BusinessException.ROLE_NOT_EXIST.newInstance("角色不存在");
        }
    }

    /**
     * 校验并去重权限ID列表
     * <p>列表为 null 或包含 null 元素时抛出参数异常；否则返回去重后的列表。</p>
     *
     * @param permissionIds 原始权限ID列表
     * @return 去重后的权限ID列表
     */
    private List<Long> distinctPermissionIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.stream().anyMatch(Objects::isNull)) {
            throw BusinessException.PARAMS_ERROR.newInstance("权限ID列表不能为空");
        }
        return permissionIds.stream().distinct().toList();
    }

    /**
     * 校验权限ID是否全部有效
     * <p>通过批量查询数据库，若查出的数量与传入数量不一致，说明存在无效权限ID。</p>
     *
     * @param permissionIds 权限ID列表
     */
    private void checkPermissionsExist(List<Long> permissionIds) {
        if (permissionIds.isEmpty()) {
            return;
        }
        List<SysPermission> permissions = sysPermissionMapper.selectBatchIds(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw BusinessException.PERMISSION_NOT_EXIST.newInstance("存在无效的权限ID");
        }
    }

    /**
     * 构建角色-权限关联实体对象
     *
     * @param roleId       角色ID
     * @param permissionId 权限ID
     * @param now          创建时间
     * @return 角色-权限关联实体
     */
    private SysRolePermission buildRolePermission(Long roleId, Long permissionId, LocalDateTime now) {
        SysRolePermission rolePermission = new SysRolePermission();
        rolePermission.setRoleId(roleId);
        rolePermission.setPermissionId(permissionId);
        rolePermission.setCreatedAt(now);
        return rolePermission;
    }
}