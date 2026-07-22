import request from '@/utils/request'

/**
 * 课程管理接口 - 对应后端 SysCourseController
 * 基础路径: /emp/sysCourse
 */

// 分页查询课程
export function pageCourse(params) {
  return request({
    url: '/emp/sysCourse/page',
    method: 'get',
    params
  })
}

// 查询课程详情
export function getCourse(id) {
  return request({
    url: `/emp/sysCourse/get/${id}`,
    method: 'get'
  })
}

// 新增课程
export function addCourse(data) {
  return request({
    url: '/emp/sysCourse/add',
    method: 'post',
    data
  })
}

// 修改课程
export function updateCourse(id, data) {
  return request({
    url: `/emp/sysCourse/update/${id}`,
    method: 'put',
    data
  })
}

// 删除课程
export function deleteCourse(id) {
  return request({
    url: `/emp/sysCourse/delete/${id}`,
    method: 'delete'
  })
}
