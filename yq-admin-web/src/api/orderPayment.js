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

export function updateOrderPayment(id, data) {
  return request({
    url: `${baseUrl}/update/${id}`,
    method: 'put',
    data
  })
}

export function deleteOrderPayment(id) {
  return request({
    url: `${baseUrl}/delete/${id}`,
    method: 'delete'
  })
}
