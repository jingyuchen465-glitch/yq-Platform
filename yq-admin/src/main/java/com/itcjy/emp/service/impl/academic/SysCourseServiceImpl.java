package com.itcjy.emp.service.impl.academic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.academic.SysCourseMapper;
import com.itcjy.emp.pojo.entity.SysCourse;
import com.itcjy.emp.pojo.entity.SysCourseDetail;
import com.itcjy.emp.pojo.req.academic.SysCoursePageReq;
import com.itcjy.emp.pojo.req.academic.SysCourseReq;
import com.itcjy.emp.pojo.req.academic.SysCourseUpdateReq;
import com.itcjy.emp.service.academic.ISysCourseDetailService;
import com.itcjy.emp.service.academic.ISysCourseService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class SysCourseServiceImpl extends ServiceImpl<SysCourseMapper, SysCourse> implements ISysCourseService {

    @Resource
    private ISysCourseDetailService sysCourseDetailService;

    @Override
    public void addCourse(SysCourseReq req) {
        SysCourse course = BeanUtil.copyProperties(req, SysCourse.class);
        this.save(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long id) {
        SysCourse course = this.getById(id);
        if (course == null) {
            throw BusinessException.COURSE_NOT_EXIST.newInstance("课程不存在");
        }
        // 级联删除该课程下所有详情记录
        sysCourseDetailService.remove(Wrappers.<SysCourseDetail>lambdaQuery()
                .eq(SysCourseDetail::getCourseId, id));
        this.removeById(id);
    }

    @Override
    public void updateCourse(Long id, SysCourseUpdateReq req) {
        SysCourse course = this.getById(id);
        if (course == null) {
            throw BusinessException.COURSE_NOT_EXIST.newInstance("课程不存在");
        }
        SysCourse updateCourse = BeanUtil.copyProperties(req, SysCourse.class);
        updateCourse.setId(id);
        this.updateById(updateCourse);
    }

    @Override
    public PageResult<SysCourse> pageCourse(SysCoursePageReq req) {
        IPage<SysCourse> page = this.page(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<SysCourse>lambdaQuery()
                        .like(StrUtil.isNotBlank(req.getCourseName()), SysCourse::getCourseName, req.getCourseName())
                        .eq(StrUtil.isNotBlank(req.getTeachingMode()), SysCourse::getTeachingMode, req.getTeachingMode())
                        .orderByDesc(SysCourse::getId)
        );
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(page.getTotal(), Collections.emptyList());
        }
        return new PageResult<>(page.getTotal(), page.getRecords());
    }
}
