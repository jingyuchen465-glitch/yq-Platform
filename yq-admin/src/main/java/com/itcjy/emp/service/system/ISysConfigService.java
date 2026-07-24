package com.itcjy.emp.service.system;

import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.system.ClassScheduleRuleReq;
import com.itcjy.emp.pojo.req.system.SysConfigItemPageReq;
import com.itcjy.emp.pojo.req.system.SysConfigItemReq;
import com.itcjy.emp.pojo.req.system.SysConfigTypePageReq;
import com.itcjy.emp.pojo.req.system.SysConfigTypeReq;
import com.itcjy.emp.pojo.res.system.ClassScheduleRuleRes;
import com.itcjy.emp.pojo.res.system.SysConfigItemRes;
import com.itcjy.emp.pojo.res.system.SysConfigTypeRes;

import java.util.List;

public interface ISysConfigService {
    void addType(SysConfigTypeReq req);
    void updateType(Long id, SysConfigTypeReq req);
    void deleteType(Long id);
    SysConfigTypeRes getType(Long id);
    PageResult<SysConfigTypeRes> pageTypes(SysConfigTypePageReq req);
    List<SysConfigTypeRes> listTypes();

    void addItem(SysConfigItemReq req);
    void updateItem(Long id, SysConfigItemReq req);
    void deleteItem(Long id);
    SysConfigItemRes getItem(Long id);
    PageResult<SysConfigItemRes> pageItems(SysConfigItemPageReq req);
    List<SysConfigItemRes> listItems(Long typeId);

    ClassScheduleRuleRes getClassScheduleRule();
    void updateClassScheduleRule(ClassScheduleRuleReq req);
}
