import request from '@/utils/request'

export function getClassDutyOptions() {
  return request({
    url: '/emp/class-duties/options',
    method: 'get'
  })
}

export function getDailyClassDuties(params) {
  return request({
    url: '/emp/class-duties/daily',
    method: 'get',
    params
  })
}

export function saveClassDuty(data) {
  return request({
    url: '/emp/class-duties',
    method: 'put',
    data
  })
}

export function deleteClassDuty(id) {
  return request({
    url: `/emp/class-duties/${id}`,
    method: 'delete'
  })
}
