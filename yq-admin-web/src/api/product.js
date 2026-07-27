import request from '@/utils/request'

/**
 * 产品管理接口 - 对应后端 MarketProductController
 * 基础路径: /emp/marketProduct
 */

export function pageProduct(params) {
  return request({
    url: '/emp/marketProduct/page',
    method: 'get',
    params
  })
}

export function getProduct(id) {
  return request({
    url: `/emp/marketProduct/get/${id}`,
    method: 'get'
  })
}

export function getProductCourseOptions() {
  return request({
    url: '/emp/marketProduct/courseOptions',
    method: 'get'
  })
}

export function addProduct(data) {
  return request({
    url: '/emp/marketProduct/add',
    method: 'post',
    data
  })
}

export function updateProduct(id, data) {
  return request({
    url: `/emp/marketProduct/update/${id}`,
    method: 'put',
    data
  })
}

export function deleteProduct(id) {
  return request({
    url: `/emp/marketProduct/delete/${id}`,
    method: 'delete'
  })
}
