import request from '@/utils/request'

/**
 * 权限管理接口 - 对应后端 SysPermissionController
 * 基础路径: /emp/sysPermission
 */

// 查询全部权限列表（不分页）
export function listPermissions(params) {
  return request({
    url: '/emp/sysPermission/list',
    method: 'get',
    params
  })
}

// 分页查询权限
export function pagePermissions(params) {
  return request({
    url: '/emp/sysPermission/page',
    method: 'get',
    params
  })
}

// 查询权限详情
export function getPermission(id) {
  return request({
    url: `/emp/sysPermission/get/${id}`,
    method: 'get'
  })
}

// 切换权限状态
export function updatePermissionStatus(id, status) {
  return request({
    url: `/emp/sysPermission/updateStatus/${id}`,
    method: 'put',
    params: { status }
  })
}
