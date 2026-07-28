import request from '@/utils/request'

export function getCurrentStudentPrepaymentOrders() {
  return request({ url: '/stu/prepayment-orders', method: 'get' })
}

export function createAlipayTrade(prepaymentOrderId) {
  return request({ url: `/stu/payments/${prepaymentOrderId}/alipay`, method: 'post' })
}

export function getPaymentDetail(paymentOrderId) {
  return request({ url: `/stu/payments/${paymentOrderId}`, method: 'get' })
}

export function createRefundRequest(paymentOrderId, data) {
  return request({ url: `/stu/payments/${paymentOrderId}/refund-requests`, method: 'post', data })
}
