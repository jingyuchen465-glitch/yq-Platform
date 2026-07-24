import request from '@/utils/request'

export function listConfigTypes() {
  return request({
    url: '/emp/sysConfig/type/list',
    method: 'get'
  })
}

export function getClassScheduleRule() {
  return request({
    url: '/emp/sysConfig/class-schedule-rule',
    method: 'get'
  })
}

export function updateClassScheduleRule(data) {
  return request({
    url: '/emp/sysConfig/class-schedule-rule',
    method: 'put',
    data
  })
}
