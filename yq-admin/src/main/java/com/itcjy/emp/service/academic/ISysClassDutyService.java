package com.itcjy.emp.service.academic;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.emp.pojo.entity.SysClassDuty;
import com.itcjy.emp.pojo.req.academic.SysClassDutyDailyReq;
import com.itcjy.emp.pojo.req.academic.SysClassDutySaveReq;
import com.itcjy.emp.pojo.res.academic.SysClassDutyDailyRes;
import com.itcjy.emp.pojo.res.academic.SysClassDutyOptionsRes;

public interface ISysClassDutyService extends IService<SysClassDuty> {

    SysClassDutyOptionsRes listOptions();

    SysClassDutyDailyRes getDailyDuties(SysClassDutyDailyReq req);

    void saveDuty(SysClassDutySaveReq req);

    void deleteDuty(Long id);
}
