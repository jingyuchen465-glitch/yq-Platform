package com.itcjy.emp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.emp.pojo.entity.SysClassSchedule;
import com.itcjy.emp.pojo.req.SysClassScheduleGenerateReq;
import com.itcjy.emp.pojo.req.SysClassScheduleTeacherAssignReq;
import com.itcjy.emp.pojo.req.SysClassScheduleTemporaryCourseReq;
import com.itcjy.emp.pojo.req.SysClassScheduleUpdateReq;
import com.itcjy.emp.pojo.res.SysClassScheduleGenerateRes;
import com.itcjy.emp.pojo.res.SysClassScheduleRes;
import com.itcjy.emp.pojo.res.SysClassScheduleTeacherAssignRes;
import com.itcjy.emp.pojo.res.SysClassScheduleTeacherAssignmentOptionsRes;
import com.itcjy.emp.pojo.res.SysClassScheduleTemporaryCourseOptionsRes;
import com.itcjy.emp.pojo.res.SysClassScheduleTemporaryCourseRes;

import java.util.List;

public interface ISysClassScheduleService extends IService<SysClassSchedule> {

    SysClassScheduleGenerateRes generateSchedule(SysClassScheduleGenerateReq req);

    List<SysClassScheduleRes> listSchedule(Long classId);

    SysClassScheduleRes getSchedule(Long scheduleId);

    SysClassScheduleRes updateSchedule(Long scheduleId, SysClassScheduleUpdateReq req);

    void deleteSchedule(Long scheduleId);

    SysClassScheduleTeacherAssignmentOptionsRes listTeacherAssignmentOptions(Long classId);

    SysClassScheduleTeacherAssignRes assignTeacherByStage(SysClassScheduleTeacherAssignReq req);

    SysClassScheduleTemporaryCourseOptionsRes listTemporaryCourseOptions(Long classId);

    SysClassScheduleTemporaryCourseRes addTemporaryCourse(SysClassScheduleTemporaryCourseReq req);

    void deleteByClassId(Long classId);
}
