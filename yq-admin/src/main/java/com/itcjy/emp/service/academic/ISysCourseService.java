package com.itcjy.emp.service.academic;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysCourse;
import com.itcjy.emp.pojo.req.academic.SysCoursePageReq;
import com.itcjy.emp.pojo.req.academic.SysCourseReq;
import com.itcjy.emp.pojo.req.academic.SysCourseUpdateReq;

public interface ISysCourseService extends IService<SysCourse> {

    /**
     * 新增课程
     */
    void addCourse(SysCourseReq req);

    /**
     * 根据 ID 删除课程
     */
    void deleteCourse(Long id);

    /**
     * 根据 ID 更新课程信息
     */
    void updateCourse(Long id, SysCourseUpdateReq req);

    /**
     * 分页查询课程
     */
    PageResult<SysCourse> pageCourse(SysCoursePageReq req);
}
