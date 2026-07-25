import request from '@/utils/request'

const baseUrl = '/emp/course-homework-templates'

export function pageCourseHomeworkTemplates(params) {
  return request({ url: baseUrl, method: 'get', params })
}

export function listTemplateCourseOptions() {
  return request({ url: `${baseUrl}/course-options`, method: 'get' })
}

export function listTemplatePositionOptions(courseId) {
  return request({
    url: `${baseUrl}/position-options`,
    method: 'get',
    params: { courseId }
  })
}

export function createCourseHomeworkTemplate(data) {
  return request({ url: baseUrl, method: 'post', data })
}

export function updateCourseHomeworkTemplate(id, data) {
  return request({ url: `${baseUrl}/${id}`, method: 'put', data })
}

export function deleteCourseHomeworkTemplate(id) {
  return request({ url: `${baseUrl}/${id}`, method: 'delete' })
}

export function getCourseHomeworkTemplatePreview(id) {
  return request({ url: `${baseUrl}/${id}/preview`, method: 'get' })
}

export function getCourseHomeworkTemplateDownload(id) {
  return request({ url: `${baseUrl}/${id}/download`, method: 'get' })
}
