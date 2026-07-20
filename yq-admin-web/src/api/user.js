import request from '@/utils/request'

/**
 * 用户管理接口 - 对应后端 SysUserController / SysUserRoleController
 * 基础路径: /emp/sysUser
 */

// 分页查询用户
export function pageUsers(params) {
  return request({
    url: '/emp/sysUser/page',
    method: 'get',
    params
  })
}

// 查询用户详情
export function getUser(id) {
  return request({
    url: `/emp/sysUser/get/${id}`,
    method: 'get'
  })
}

// 添加用户
export function addUser(data) {
  return request({
    url: '/emp/sysUser/add',
    method: 'post',
    data
  })
}

// 修改用户
export function updateUser(id, data) {
  return request({
    url: `/emp/sysUser/update/${id}`,
    method: 'put',
    data
  })
}

// 删除用户
export function deleteUser(id) {
  return request({
    url: `/emp/sysUser/delete/${id}`,
    method: 'delete'
  })
}

// 批量删除用户
export function batchDeleteUsers(ids) {
  return request({
    url: '/emp/sysUser/batchDelete',
    method: 'delete',
    data: ids
  })
}

// 查询用户已拥有角色ID列表
export function getUserRoleIds(userId) {
  return request({
    url: `/emp/sysUser/${userId}/roles`,
    method: 'get'
  })
}

// 用户分配角色（全量覆盖）
export function assignUserRoles(userId, roleIds) {
  return request({
    url: `/emp/sysUser/${userId}/roles`,
    method: 'put',
    data: { roleIds }
  })
}
