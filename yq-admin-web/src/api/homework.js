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

export function pageHomeworkSubmissions(homeworkId, params) {
  return request({
    url: `/emp/homeworks/${homeworkId}/submissions`,
    method: 'get',
    params
  })
}

export function getHomeworkSubmissionDownloadUrl(submissionId, preview) {
  return request({
    url: `/emp/homework-submissions/${submissionId}/download-url`,
    method: 'get',
    params: { preview }
  })
}

export function gradeHomeworkSubmission(submissionId, data) {
  return request({
    url: `/emp/homework-submissions/${submissionId}/grading`,
    method: 'patch',
    data
  })
}
