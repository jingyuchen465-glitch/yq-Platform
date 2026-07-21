import axios from 'axios'
import { Message } from 'element-ui'
import router from '@/router'

// 创建 axios 实例，baseURL 走 devServer 代理
const service = axios.create({
  baseURL: '/yq-admin',
  timeout: 15000
})

// 请求拦截器：自动携带 token
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('yq_token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理后端 ApiResponse 结构 { code, message, data }
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      Message({
        message: res.message || '请求失败',
        type: 'error',
        duration: 3000
      })
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    let msg = '网络异常，请稍后重试'
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        // token 过期或无效，清除登录态并跳转登录页
        localStorage.removeItem('yq_token')
        localStorage.removeItem('yq_sign_secret')
        localStorage.removeItem('yq_user')
        router.push('/login')
        msg = '登录已过期，请重新登录'
      } else if (data && data.message) {
        msg = data.message
      } else if (status === 404) {
        msg = '请求的资源不存在'
      } else if (status === 500) {
        msg = '服务器内部错误'
      }
    }
    Message({
      message: msg,
      type: 'error',
      duration: 3000
    })
    return Promise.reject(error)
  }
)

export default service
