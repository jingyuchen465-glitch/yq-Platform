package com.itcjy.emp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.listener.CourseDetailImportListener;
import com.itcjy.emp.mapper.SysCourseDetailMapper;
import com.itcjy.emp.mapper.SysCourseMapper;
import com.itcjy.emp.pojo.entity.SysCourse;
import com.itcjy.emp.pojo.entity.SysCourseDetail;
import com.itcjy.emp.pojo.excel.CourseDetailExcelData;
import com.itcjy.emp.pojo.req.SysCourseDetailPageReq;
import com.itcjy.emp.pojo.req.SysCourseDetailReq;
import com.itcjy.emp.pojo.req.SysCourseDetailUpdateReq;
import com.itcjy.emp.service.ISysCourseDetailService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class SysCourseDetailServiceImpl extends ServiceImpl<SysCourseDetailMapper, SysCourseDetail> implements ISysCourseDetailService {

    @Resource
    private SysCourseMapper sysCourseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCourseDetail(SysCourseDetailReq req) {
        // 校验课程是否存在
        SysCourse course = sysCourseMapper.selectById(req.getCourseId());
        if (course == null) {
            throw BusinessException.COURSE_NOT_EXIST.newInstance("课程不存在");
        }
        // 新增到指定天数时，将当前课程中该天及之后的课程详情整体后移一天。
        // 例如已有 D1、D2、D3、D4，新增 D2 时，原 D2/D3/D4 会顺延为 D3/D4/D5。
        shiftDayNumberAfter(req.getCourseId(), req.getDayNumber());
        SysCourseDetail detail = BeanUtil.copyProperties(req, SysCourseDetail.class);
        this.save(detail);
    }

    /**
     * 将指定课程中 fromDay 及之后的课程详情顺延一天，避免新增时出现重复天数。
     */
    private void shiftDayNumberAfter(Long courseId, Integer fromDay) {
        if (fromDay == null) {
            return;
        }
        this.update(Wrappers.<SysCourseDetail>lambdaUpdate()
                .eq(SysCourseDetail::getCourseId, courseId)
                .ge(SysCourseDetail::getDayNumber, fromDay)
                .setSql("day_number = day_number + 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourseDetail(Long id) {
        SysCourseDetail detail = this.getById(id);
        if (detail == null) {
            throw BusinessException.COURSE_DETAIL_NOT_EXIST.newInstance("课程详情不存在");
        }
        this.removeById(id);
        // 删除指定天数后，将当前课程中后续课程详情整体前移一天。
        // 例如已有 D1、D2、D3、D4，删除 D2 后，原 D3/D4 会调整为 D2/D3。
        shiftDayNumberBefore(detail.getCourseId(), detail.getDayNumber());
    }

    /**
     * 将指定课程中 afterDay 之后的课程详情前移一天，避免删除后出现天数断档。
     */
    private void shiftDayNumberBefore(Long courseId, Integer afterDay) {
        if (afterDay == null) {
            return;
        }
        this.update(Wrappers.<SysCourseDetail>lambdaUpdate()
                .eq(SysCourseDetail::getCourseId, courseId)
                .gt(SysCourseDetail::getDayNumber, afterDay)
                .setSql("day_number = day_number - 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourseDetails(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.PARAMS_ERROR.newInstance("删除ID列表不能为空");
        }
        List<Long> distinctIds = ids.stream().distinct().toList();
        if (distinctIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw BusinessException.PARAMS_ERROR.newInstance("课程详情ID列表不正确");
        }

        List<SysCourseDetail> details = this.listByIds(distinctIds);
        if (details.size() != distinctIds.size()) {
            throw BusinessException.COURSE_DETAIL_NOT_EXIST.newInstance("部分课程详情不存在");
        }

        List<Long> affectedCourseIds = details.stream()
                .map(SysCourseDetail::getCourseId)
                .distinct()
                .toList();
        this.removeByIds(distinctIds);
        affectedCourseIds.forEach(this::normalizeDayNumbers);
    }

    /**
     * 将指定课程下剩余课程详情按当前顺序重新压缩为 D1、D2、D3...，避免批量删除后出现天数断档。
     */
    private void normalizeDayNumbers(Long courseId) {
        List<SysCourseDetail> details = this.list(Wrappers.<SysCourseDetail>lambdaQuery()
                .eq(SysCourseDetail::getCourseId, courseId)
                .isNotNull(SysCourseDetail::getDayNumber));
        if (details.isEmpty()) {
            return;
        }
        details.sort(Comparator
                .comparing(SysCourseDetail::getDayNumber)
                .thenComparing(Comparator.comparing(SysCourseDetail::getId).reversed()));

        List<SysCourseDetail> needUpdate = new ArrayList<>();
        for (int i = 0; i < details.size(); i++) {
            SysCourseDetail detail = details.get(i);
            int newDayNumber = i + 1;
            if (!Integer.valueOf(newDayNumber).equals(detail.getDayNumber())) {
                SysCourseDetail updateDetail = new SysCourseDetail();
                updateDetail.setId(detail.getId());
                updateDetail.setDayNumber(newDayNumber);
                needUpdate.add(updateDetail);
            }
        }
        if (!needUpdate.isEmpty()) {
            this.updateBatchById(needUpdate);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCourseDetail(Long id, SysCourseDetailUpdateReq req) {
        SysCourseDetail detail = this.getById(id);
        if (detail == null) {
            throw BusinessException.COURSE_DETAIL_NOT_EXIST.newInstance("课程详情不存在");
        }
        // 处理 dayNumber 变更时的天数冲突
        Integer oldDay = detail.getDayNumber();
        Integer newDay = req.getDayNumber();
        if (newDay != null && !newDay.equals(oldDay)) {
            Long courseId = detail.getCourseId();
            // 先移除旧天数占位：旧天数之后的记录前移一天
            if (oldDay != null) {
                shiftDayNumberBefore(courseId, oldDay);
            }
            // 再插入新天数位置：新天数及之后的记录后移一天
            shiftDayNumberAfter(courseId, newDay);
        }
        SysCourseDetail updateDetail = BeanUtil.copyProperties(req, SysCourseDetail.class);
        updateDetail.setId(id);
        this.updateById(updateDetail);
    }

    @Override
    public PageResult<SysCourseDetail> pageCourseDetail(SysCourseDetailPageReq req) {
        IPage<SysCourseDetail> page = this.page(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<SysCourseDetail>lambdaQuery()
                        .eq(SysCourseDetail::getCourseId, req.getCourseId())
                        .like(StrUtil.isNotBlank(req.getStageName()), SysCourseDetail::getStageName, req.getStageName())
                        .orderByAsc(SysCourseDetail::getDayNumber)
                        .orderByDesc(SysCourseDetail::getId)
        );
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(page.getTotal(), Collections.emptyList());
        }
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importCourseDetail(Long courseId, MultipartFile file) {
        // 校验课程是否存在
        SysCourse course = sysCourseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.COURSE_NOT_EXIST.newInstance("课程不存在");
        }
        // 流式读取 + 分批入库，避免大文件 OOM
        CourseDetailImportListener listener = new CourseDetailImportListener(courseId, this::saveBatch);
        try {
            EasyExcel.read(file.getInputStream(), CourseDetailExcelData.class, listener)
                    .sheet()
                    .doRead();
        } catch (IOException e) {
            throw BusinessException.PARAMS_ERROR.newInstance("读取 Excel 文件失败");
        }
        if (listener.getTotalCount() == 0) {
            throw BusinessException.PARAMS_ERROR.newInstance("Excel 文件内容为空");
        }
        return listener.getTotalCount();
    }
}
