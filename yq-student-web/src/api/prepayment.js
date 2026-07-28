import request from '@/utils/request'

export function getCurrentStudentPrepaymentOrders() {
  return request({ url: '/stu/prepayment-orders', method: 'get' })
}

export function createAlipayTrade(prepaymentOrderId) {
  return request({ url: `/stu/payments/${prepaymentOrderId}/alipay`, method: 'post' })
}
