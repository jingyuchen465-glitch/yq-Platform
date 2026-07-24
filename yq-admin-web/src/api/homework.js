import request from '@/utils/request'

export function listHomeworkClassOptions() {
  return request({
    url: '/emp/homeworks/class-options',
    method: 'get'
  })
}

export function prefillHomework(params) {
  return request({
    url: '/emp/homeworks/prefill',
    method: 'get',
    params
  })
}

export function publishHomework(data) {
  return request({
    url: '/emp/homeworks',
    method: 'post',
    data
  })
}

export function pageHomeworkStatus(params) {
  return request({
    url: '/emp/homeworks/statuses',
    method: 'get',
    params
  })
}

export function saveHomeworkAnswer(homeworkId, data) {
  return request({
    url: `/emp/homeworks/${homeworkId}/answer`,
    method: 'put',
    data
  })
}

export function updateHomeworkAnswerVisibility(homeworkId, data) {
  return request({
    url: `/emp/homeworks/${homeworkId}/answer-visibility`,
    method: 'patch',
    data
  })
}
