package com.itcjy.emp.service.academic;

import com.itcjy.emp.pojo.req.academic.SysTeacherScheduleCalendarReq;
import com.itcjy.emp.pojo.res.academic.SysTeacherScheduleCalendarRes;

public interface ISysTeacherScheduleService {
    SysTeacherScheduleCalendarRes getTeacherCalendar(SysTeacherScheduleCalendarReq req);
}
