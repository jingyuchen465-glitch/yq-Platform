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

export function listConfigItems(typeId) {
  return request({
    url: `/emp/sysConfig/item/list/${typeId}`,
    method: 'get'
  })
}

export function addConfigItem(data) {
  return request({
    url: '/emp/sysConfig/item/add',
    method: 'post',
    data
  })
}

export function updateConfigItem(id, data) {
  return request({
    url: `/emp/sysConfig/item/update/${id}`,
    method: 'put',
    data
  })
}

export function deleteConfigItem(id) {
  return request({
    url: `/emp/sysConfig/item/delete/${id}`,
    method: 'delete'
  })
}
