import request from '@/utils/request'

const baseUrl = '/emp/payment-refunds'

export function pagePaymentRefunds(params) {
  return request({ url: `${baseUrl}/page`, method: 'get', params })
}

export function approvePaymentRefund(id, data) {
  return request({ url: `${baseUrl}/${id}/approve`, method: 'post', data })
}

export function rejectPaymentRefund(id, data) {
  return request({ url: `${baseUrl}/${id}/reject`, method: 'post', data })
}
