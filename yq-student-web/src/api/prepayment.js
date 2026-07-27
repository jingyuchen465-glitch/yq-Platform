import request from '@/utils/request'

export function getCurrentStudentPrepaymentOrders() {
  return request({ url: '/stu/prepayment-orders', method: 'get' })
}

export function createAlipayTrade(data) {
  return request({ url: '/pay/alipay/create', method: 'post', data })
}
