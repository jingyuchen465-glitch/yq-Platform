package com.itcjy.emp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.emp.pojo.entity.SysClassSchedule;
import com.itcjy.emp.pojo.req.SysClassScheduleGenerateReq;
import com.itcjy.emp.pojo.res.SysClassScheduleGenerateRes;
import com.itcjy.emp.pojo.res.SysClassScheduleRes;

import java.util.List;

public interface ISysClassScheduleService extends IService<SysClassSchedule> {

    SysClassScheduleGenerateRes generateSchedule(SysClassScheduleGenerateReq req);

    List<SysClassScheduleRes> listSchedule(Long classId);

    void deleteByClassId(Long classId);
}
