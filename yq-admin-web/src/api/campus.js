import request from '@/utils/request'

/**
 * 校区管理接口 - 对应后端 SysCampusController
 * 基础路径: /emp/sysCampus
 */

// 分页查询校区
export function pageCampus(params) {
  return request({
    url: '/emp/sysCampus/page',
    method: 'get',
    params
  })
}

// 查询校区详情
export function getCampus(id) {
  return request({
    url: `/emp/sysCampus/get/${id}`,
    method: 'get'
  })
}

// 新增校区
export function addCampus(data) {
  return request({
    url: '/emp/sysCampus/add',
    method: 'post',
    data
  })
}

// 修改校区
export function updateCampus(id, data) {
  return request({
    url: `/emp/sysCampus/update/${id}`,
    method: 'put',
    data
  })
}

// 删除校区
export function deleteCampus(id) {
  return request({
    url: `/emp/sysCampus/delete/${id}`,
    method: 'delete'
  })
}
