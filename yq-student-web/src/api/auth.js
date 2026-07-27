import request from '@/utils/request'

export function login(data) {
  return request({ url: '/stu/login', method: 'post', data })
}

export function getCurrentStudent() {
  return request({ url: '/stu/me', method: 'get' })
}

export function logout() {
  return request({ url: '/stu/logout', method: 'post' })
}
