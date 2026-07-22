import request from '@/utils/request'

/**
 * 班级管理接口 - 对应后端 SysClassController
 * 基础路径: /emp/sysClass
 */

export function pageClass(params) {
  return request({
    url: '/emp/sysClass/page',
    method: 'get',
    params
  })
}

export function getClassDetail(id) {
  return request({
    url: `/emp/sysClass/get/${id}`,
    method: 'get'
  })
}

export function getClassFormOptions() {
  return request({
    url: '/emp/sysClass/formOptions',
    method: 'get'
  })
}

export function addClass(data) {
  return request({
    url: '/emp/sysClass/add',
    method: 'post',
    data
  })
}

export function updateClass(id, data) {
  return request({
    url: `/emp/sysClass/update/${id}`,
    method: 'put',
    data
  })
}

export function deleteClass(id) {
  return request({
    url: `/emp/sysClass/delete/${id}`,
    method: 'delete'
  })
}

export function generateClassSchedule(data) {
  return request({
    url: '/emp/sysClassSchedule/generate',
    method: 'post',
    data
  })
}

export function listClassSchedule(classId) {
  return request({
    url: `/emp/sysClassSchedule/list/${classId}`,
    method: 'get'
  })
}

export function getClassSchedule(scheduleId) {
  return request({
    url: `/emp/sysClassSchedule/get/${scheduleId}`,
    method: 'get'
  })
}

export function updateClassSchedule(scheduleId, data) {
  return request({
    url: `/emp/sysClassSchedule/update/${scheduleId}`,
    method: 'put',
    data
  })
}

export function deleteClassSchedule(scheduleId) {
  return request({
    url: `/emp/sysClassSchedule/delete/${scheduleId}`,
    method: 'delete'
  })
}

export function getScheduleTeacherAssignmentOptions(classId) {
  return request({
    url: `/emp/sysClassSchedule/teacherAssignmentOptions/${classId}`,
    method: 'get'
  })
}

export function assignScheduleTeacher(data) {
  return request({
    url: '/emp/sysClassSchedule/assignTeacher',
    method: 'put',
    data
  })
}

export function getTemporaryCourseOptions(classId) {
  return request({
    url: `/emp/sysClassSchedule/temporaryCourseOptions/${classId}`,
    method: 'get'
  })
}

export function addTemporaryCourse(data) {
  return request({
    url: '/emp/sysClassSchedule/temporaryCourse',
    method: 'post',
    data
  })
}
