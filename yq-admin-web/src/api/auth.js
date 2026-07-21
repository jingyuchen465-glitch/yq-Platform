import request from '@/utils/request'

/**
 * 认证接口 - 对应后端 SysUserController#empLogin
 * 基础路径: /emp/sysUser
 */

// 员工登录
export function login(data) {
  return request({
    url: '/emp/sysUser/login',
    method: 'post',
    data
  })
}

// 退出登录
export function logout() {
  return request({
    url: '/emp/sysUser/logout',
    method: 'post'
  })
}
