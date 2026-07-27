import request from '@/utils/request'

const baseUrl = '/emp/prepaymentOrder'

export function pagePrepaymentOrder(params) {
  return request({
    url: `${baseUrl}/page`,
    method: 'get',
    params
  })
}

export function getPrepaymentOrder(id) {
  return request({
    url: `${baseUrl}/get/${id}`,
    method: 'get'
  })
}

export function getSalespersonOptions() {
  return request({
    url: `${baseUrl}/salespersonOptions`,
    method: 'get'
  })
}

export function getPrepaymentProductOptions() {
  return request({
    url: `${baseUrl}/productOptions`,
    method: 'get'
  })
}

export function getEducationOptions() {
  return request({
    url: `${baseUrl}/educationOptions`,
    method: 'get'
  })
}

export function addPrepaymentOrder(data) {
  return request({
    url: `${baseUrl}/add`,
    method: 'post',
    data
  })
}

export function updatePrepaymentOrder(id, data) {
  return request({
    url: `${baseUrl}/update/${id}`,
    method: 'put',
    data
  })
}

export function deletePrepaymentOrder(id) {
  return request({
    url: `${baseUrl}/delete/${id}`,
    method: 'delete'
  })
}
