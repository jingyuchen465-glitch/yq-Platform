package com.itcjy.emp.service.academic;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.emp.pojo.entity.SysClassSchedule;
import com.itcjy.emp.pojo.req.academic.SysClassScheduleGenerateReq;
import com.itcjy.emp.pojo.req.academic.SysClassScheduleTeacherAssignReq;
import com.itcjy.emp.pojo.req.academic.SysClassScheduleTemporaryCourseReq;
import com.itcjy.emp.pojo.req.academic.SysClassScheduleUpdateReq;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleGenerateRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTeacherAssignRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTeacherAssignmentOptionsRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTemporaryCourseOptionsRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTemporaryCourseRes;

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
