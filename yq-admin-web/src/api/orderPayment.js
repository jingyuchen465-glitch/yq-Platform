import request from '@/utils/request'

const baseUrl = '/emp/orderPayment'

export function pageOrderPayment(params) {
  return request({
    url: `${baseUrl}/page`,
    method: 'get',
    params
  })
}

export function getOrderPayment(id) {
  return request({
    url: `${baseUrl}/get/${id}`,
    method: 'get'
  })
}
