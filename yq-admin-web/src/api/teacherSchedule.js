import request from '@/utils/request'

export function getTeacherScheduleCalendar(params) {
  return request({
    url: '/emp/teacher-schedules/calendar',
    method: 'get',
    params
  })
}
