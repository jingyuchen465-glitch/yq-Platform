import axios from 'axios'
import { Message, MessageBox } from 'element-ui'
import router from '@/router'
import { buildRequestSign, buildQueryString } from '@/utils/sign'

// 创建 axios 实例，baseURL 走 devServer 代理
const service = axios.create({
  baseURL: '/yq-admin',
  timeout: 15000
})

// 请求拦截器：自动携带 Authorization + 签名三要素（X-Timestamp / X-Nonce / X-Sign）
service.interceptors.request.use(
  async config => {
    const token = localStorage.getItem('yq_token')
    const signSecret = localStorage.getItem('yq_sign_secret')

    if (token && signSecret) {
      // 先自行序列化查询参数，保证签名原文与实际发出的 query 完全一致
      const query = buildQueryString(config.params)
      if (config.params) {
        config.paramsSerializer = () => query
      }

      // 后端 request.getRequestURI() 包含 context-path，即 baseURL + url
      const uri = (config.baseURL || '') + config.url
      const { timestamp, nonce, sign } = await buildRequestSign(
        (config.method || 'get').toUpperCase(),
        uri,
        query,
        signSecret
      )

      config.headers['Authorization'] = 'Bearer ' + token
      config.headers['X-Timestamp'] = timestamp
      config.headers['X-Nonce'] = nonce
      config.headers['X-Sign'] = sign
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 认证失效业务码（后端 GlobalExceptionHandler 统一返回 HTTP 200，需按业务码判断）
const AUTH_FAIL_CODES = [17004, 17005, 17006]
let authDialogPromise = null

function clearAuthStorage() {
  localStorage.removeItem('yq_token')
  localStorage.removeItem('yq_sign_secret')
  localStorage.removeItem('yq_user')
}

/**
 * 认证失效可能由同一页面的多个并发请求同时触发，只展示一个处理窗口。
 * 用户确认后再跳转，避免请求拦截器和路由守卫竞争导航。
 */
function handleAuthFailure(message) {
  const redirectPath = router.currentRoute.path === '/login'
    ? null
    : router.currentRoute.fullPath
  clearAuthStorage()

  if (authDialogPromise) return authDialogPromise

  authDialogPromise = MessageBox.alert(
    message || '当前登录状态已失效，请重新登录后继续操作。',
    '登录状态已失效',
    {
      confirmButtonText: '重新登录',
      type: 'warning',
      showClose: false,
      closeOnClickModal: false,
      closeOnPressEscape: false,
      customClass: 'auth-expired-dialog'
    }
  )
    .catch(() => {})
    .then(() => {
      if (router.currentRoute.path !== '/login') {
        return router.replace({
          path: '/login',
          query: redirectPath ? { redirect: redirectPath } : {}
        })
      }
      return null
    })
    .finally(() => {
      authDialogPromise = null
    })

  return authDialogPromise
}

// 响应拦截器：统一处理后端 ApiResponse 结构 { code, message, data }
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      // 认证失效：清除登录态并跳转登录页
      if (AUTH_FAIL_CODES.includes(res.code)) {
        handleAuthFailure(res.message)
      } else {
        Message({ message: res.message || '请求失败', type: 'error', duration: 3000 })
      }
      const requestError = new Error(res.message || '请求失败')
      // 保留后端业务码，页面可以针对无权限等预期异常展示友好的局部状态。
      requestError.code = res.code
      requestError.responseData = res
      return Promise.reject(requestError)
    }
    return res
  },
  error => {
    let msg = '网络异常，请稍后重试'
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        handleAuthFailure('登录已过期，请重新登录后继续操作。')
        return Promise.reject(error)
      } else if (data && data.message) {
        msg = data.message
      } else if (status === 404) {
        msg = '请求的资源不存在'
      } else if (status === 500) {
        msg = '服务器内部错误'
      }
    }
    Message({ message: msg, type: 'error', duration: 3000 })
    return Promise.reject(error)
  }
)

export default service
