import request from '@/utils/request'

const BASE_URL = '/emp/marketStudent'

export function pageStudents(params) {
  return request({
    url: `${BASE_URL}/page`,
    method: 'get',
    params
  })
}

export function getStudent(id) {
  return request({
    url: `${BASE_URL}/get/${id}`,
    method: 'get'
  })
}

export function getStudentClassOptions() {
  return request({
    url: `${BASE_URL}/classOptions`,
    method: 'get'
  })
}

export function updateStudent(id, data) {
  return request({
    url: `${BASE_URL}/update/${id}`,
    method: 'put',
    data
  })
}

export function withdrawStudent(id) {
  return request({
    url: `${BASE_URL}/delete/${id}`,
    method: 'delete'
  })
}
