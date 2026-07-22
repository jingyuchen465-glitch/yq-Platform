package com.itcjy.emp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysClass;
import com.itcjy.emp.pojo.req.SysClassPageReq;
import com.itcjy.emp.pojo.req.SysClassReq;
import com.itcjy.emp.pojo.req.SysClassUpdateReq;
import com.itcjy.emp.pojo.res.SysClassFormOptionsRes;
import com.itcjy.emp.pojo.res.SysClassRes;

public interface ISysClassService extends IService<SysClass> {

    void addClass(SysClassReq req);

    void updateClass(Long id, SysClassUpdateReq req);

    void deleteClass(Long id);

    SysClassRes getClassDetail(Long id);

    PageResult<SysClassRes> pageClasses(SysClassPageReq req);

    SysClassFormOptionsRes listFormOptions();
}
