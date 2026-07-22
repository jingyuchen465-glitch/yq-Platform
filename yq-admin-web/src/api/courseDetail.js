import request from '@/utils/request'

/**
 * 课程详情管理接口 - 对应后端 SysCourseDetailController
 * 基础路径: /emp/sysCourseDetail
 */

// 分页查询课程详情
export function pageCourseDetail(params) {
  return request({
    url: '/emp/sysCourseDetail/page',
    method: 'get',
    params
  })
}

// 新增课程详情
export function addCourseDetail(data) {
  return request({
    url: '/emp/sysCourseDetail/add',
    method: 'post',
    data
  })
}

// 修改课程详情
export function updateCourseDetail(id, data) {
  return request({
    url: `/emp/sysCourseDetail/update/${id}`,
    method: 'put',
    data
  })
}

// 删除课程详情
export function deleteCourseDetail(id) {
  return request({
    url: `/emp/sysCourseDetail/delete/${id}`,
    method: 'delete'
  })
}

// 批量删除课程详情
export function batchDeleteCourseDetails(ids) {
  return request({
    url: '/emp/sysCourseDetail/batchDelete',
    method: 'delete',
    data: ids
  })
}

// Excel 导入课程详情
export function importCourseDetail(courseId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/emp/sysCourseDetail/import',
    method: 'post',
    params: { courseId },
    data: formData
  })
}
