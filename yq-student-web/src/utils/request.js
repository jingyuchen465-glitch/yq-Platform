import axios from 'axios'
import router from '@/router'
import { notifyStudentAuthExpired } from '@/utils/authEvents'
import { buildQueryString, buildRequestSign } from '@/utils/sign'
import {
  clearStudentSession,
  getStudentSignSecret,
  getStudentToken
} from '@/utils/session'

const service = axios.create({
  baseURL: '/yq-admin',
  timeout: 15000
})

service.interceptors.request.use(async config => {
  const token = getStudentToken()
  const signSecret = getStudentSignSecret()
  if (!token || !signSecret) return config

  const query = buildQueryString(config.params)
  if (config.params) config.paramsSerializer = () => query
  const uri = `${config.baseURL || ''}${config.url}`
  const signature = await buildRequestSign(config.method || 'get', uri, query, signSecret)

  config.headers.Authorization = `Bearer ${token}`
  config.headers['X-Timestamp'] = signature.timestamp
  config.headers['X-Nonce'] = signature.nonce
  config.headers['X-Sign'] = signature.sign
  return config
})

const AUTH_FAILURE_CODES = [17004, 17005, 17006]

function redirectToLogin() {
  const redirect = router.currentRoute.path === '/login' ? null : router.currentRoute.fullPath
  clearStudentSession()
  if (router.currentRoute.path !== '/login') {
    notifyStudentAuthExpired(redirect)
  }
}

service.interceptors.response.use(response => {
  const result = response.data
  if (result.code === 200) return result
  if (AUTH_FAILURE_CODES.includes(result.code)) redirectToLogin()
  const error = new Error(result.message || '请求未完成，请稍后重试')
  error.code = result.code
  throw error
}, error => {
  if (error.response && error.response.status === 401) redirectToLogin()
  const message = error.response && error.response.data && error.response.data.message
  return Promise.reject(new Error(message || '网络连接异常，请检查后重试'))
})

export default service
