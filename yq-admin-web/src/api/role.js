import request from '@/utils/request'

/**
 * 角色管理接口 - 对应后端 SysRoleController / SysRolePermissionController
 * 基础路径: /emp/sysRole
 */

// 分页查询角色
export function pageRoles(params) {
  return request({
    url: '/emp/sysRole/page',
    method: 'get',
    params
  })
}

// 查询角色详情
export function getRole(id) {
  return request({
    url: `/emp/sysRole/get/${id}`,
    method: 'get'
  })
}

// 新增角色
export function addRole(data) {
  return request({
    url: '/emp/sysRole/add',
    method: 'post',
    data
  })
}

// 修改角色
export function updateRole(id, data) {
  return request({
    url: `/emp/sysRole/update/${id}`,
    method: 'put',
    data
  })
}

// 删除角色
export function deleteRole(id) {
  return request({
    url: `/emp/sysRole/delete/${id}`,
    method: 'delete'
  })
}

// 批量删除角色
export function batchDeleteRoles(ids) {
  return request({
    url: '/emp/sysRole/batchDelete',
    method: 'delete',
    data: ids
  })
}

// 查询角色已拥有权限ID列表
export function getRolePermissionIds(roleId) {
  return request({
    url: `/emp/sysRole/${roleId}/permissions`,
    method: 'get'
  })
}

// 角色授权（全量覆盖权限）
export function assignRolePermissions(roleId, permissionIds) {
  return request({
    url: `/emp/sysRole/${roleId}/permissions`,
    method: 'put',
    data: { permissionIds }
  })
}
