package com.itcjy.emp.service.impl.homework;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.common.myEnum.TeachingModeEnum;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.academic.SysCourseDetailMapper;
import com.itcjy.emp.mapper.academic.SysCourseMapper;
import com.itcjy.emp.mapper.homework.CourseHomeworkTemplateMapper;
import com.itcjy.emp.pojo.entity.CourseHomeworkTemplate;
import com.itcjy.emp.pojo.entity.SysCourse;
import com.itcjy.emp.pojo.entity.SysCourseDetail;
import com.itcjy.emp.pojo.req.homework.CourseHomeworkTemplateCreateReq;
import com.itcjy.emp.pojo.req.homework.CourseHomeworkTemplatePageReq;
import com.itcjy.emp.pojo.req.homework.CourseHomeworkTemplateUpdateReq;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplateCourseOptionRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplateDownloadRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplateMatchRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplatePositionRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplatePreviewRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplateRes;
import com.itcjy.emp.pojo.res.oss.OssDownloadUrlRes;
import com.itcjy.emp.service.homework.ICourseHomeworkTemplateService;
import com.itcjy.emp.service.oss.IOssService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseHomeworkTemplateServiceImpl implements ICourseHomeworkTemplateService {

    private final CourseHomeworkTemplateMapper templateMapper;
    private final SysCourseMapper courseMapper;
    private final SysCourseDetailMapper courseDetailMapper;
    private final IOssService ossService;

    @Override
    public PageResult<CourseHomeworkTemplateRes> page(CourseHomeworkTemplatePageReq req) {
        List<Long> matchedCourseIds = null;
        if (StrUtil.isNotBlank(req.getCourseName())) {
            matchedCourseIds = courseMapper.selectList(Wrappers.<SysCourse>lambdaQuery()
                            .like(SysCourse::getCourseName, req.getCourseName()))
                    .stream().map(SysCourse::getId).toList();
            if (matchedCourseIds.isEmpty()) {
                return new PageResult<>(0L, Collections.emptyList());
            }
        }

        IPage<CourseHomeworkTemplate> result = templateMapper.selectPage(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<CourseHomeworkTemplate>lambdaQuery()
                        .eq(req.getCourseId() != null, CourseHomeworkTemplate::getCourseId, req.getCourseId())
                        .in(matchedCourseIds != null, CourseHomeworkTemplate::getCourseId, matchedCourseIds)
                        .eq(StrUtil.isNotBlank(req.getStatus()), CourseHomeworkTemplate::getStatus, req.getStatus())
                        .orderByDesc(CourseHomeworkTemplate::getUpdatedAt)
                        .orderByDesc(CourseHomeworkTemplate::getId)
        );
        if (result.getRecords().isEmpty()) {
            return new PageResult<>(result.getTotal(), Collections.emptyList());
        }

        List<Long> courseIds = result.getRecords().stream()
                .map(CourseHomeworkTemplate::getCourseId).distinct().toList();
        Map<Long, String> courseNames = courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(SysCourse::getId, SysCourse::getCourseName));
        List<CourseHomeworkTemplateRes> records = result.getRecords().stream()
                .map(entity -> CourseHomeworkTemplateRes.from(entity, courseNames.get(entity.getCourseId())))
                .toList();
        return new PageResult<>(result.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseHomeworkTemplateRes create(CourseHomeworkTemplateCreateReq req) {
        TemplateBinding binding = resolveBinding(req.courseId(), req.courseDetailId());
        CourseHomeworkTemplate entity = new CourseHomeworkTemplate();
        applyBinding(entity, binding);
        entity.setContentObjectKey(req.contentObjectKey().trim());
        entity.setContentFileName(req.contentFileName().trim());
        entity.setStatus(StrUtil.blankToDefault(req.status(), ActiveEnum.ACTIVE.name()));
        entity.setRemark(StrUtil.trim(req.remark()));
        insertOrThrow(entity, binding);
        return CourseHomeworkTemplateRes.from(entity, binding.course().getCourseName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseHomeworkTemplateRes update(Long id, CourseHomeworkTemplateUpdateReq req) {
        CourseHomeworkTemplate entity = requireTemplate(id);
        TemplateBinding binding = resolveBinding(req.courseId(), req.courseDetailId());
        applyBinding(entity, binding);
        entity.setContentObjectKey(req.contentObjectKey().trim());
        entity.setContentFileName(req.contentFileName().trim());
        entity.setStatus(req.status());
        entity.setRemark(StrUtil.trim(req.remark()));
        try {
            if (templateMapper.updateById(entity) != 1) {
                throw BusinessException.DATA_ERROR.newInstance("作业标准更新失败");
            }
        } catch (DuplicateKeyException ex) {
            throw duplicateBindingException(binding);
        }
        return CourseHomeworkTemplateRes.from(entity, binding.course().getCourseName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireTemplate(id);
        if (templateMapper.deleteById(id) != 1) {
            throw BusinessException.DATA_ERROR.newInstance("作业标准删除失败");
        }
    }

    @Override
    public List<CourseHomeworkTemplateCourseOptionRes> listCourseOptions() {
        return courseMapper.selectList(Wrappers.<SysCourse>lambdaQuery()
                        .orderByAsc(SysCourse::getCourseName).orderByAsc(SysCourse::getId))
                .stream()
                .map(course -> new CourseHomeworkTemplateCourseOptionRes(
                        course.getId(), course.getCourseName(), course.getTeachingMode()))
                .toList();
    }

    @Override
    public List<CourseHomeworkTemplatePositionRes> listPositions(Long courseId) {
        SysCourse course = requireCourse(courseId);
        List<SysCourseDetail> details = courseDetailMapper.selectList(
                Wrappers.<SysCourseDetail>lambdaQuery()
                        .eq(SysCourseDetail::getCourseId, courseId)
                        .orderByAsc(SysCourseDetail::getDayNumber)
                        .orderByAsc(SysCourseDetail::getId));

        if (TeachingModeEnum.ONLINE.name().equals(course.getTeachingMode())) {
            Map<String, SysCourseDetail> uniqueStages = details.stream()
                    .filter(detail -> StrUtil.isNotBlank(detail.getStageName()))
                    .collect(Collectors.toMap(
                            SysCourseDetail::getStageName,
                            Function.identity(),
                            (first, ignored) -> first,
                            LinkedHashMap::new));
            return uniqueStages.values().stream().map(this::toPosition).toList();
        }
        return details.stream()
                .filter(detail -> detail.getDayNumber() != null)
                .map(this::toPosition)
                .toList();
    }

    @Override
    public CourseHomeworkTemplatePreviewRes getPreview(Long id) {
        CourseHomeworkTemplate template = requireTemplate(id);
        return toPreview(template);
    }

    @Override
    public CourseHomeworkTemplateDownloadRes getDownload(Long id) {
        CourseHomeworkTemplate template = requireTemplate(id);
        OssDownloadUrlRes signed = ossService.generateDownloadUrl(
                template.getContentObjectKey(), false, template.getContentFileName());
        return new CourseHomeworkTemplateDownloadRes(
                template.getId(), template.getContentFileName(), signed.downloadUrl(), signed.expireSeconds());
    }

    @Override
    public CourseHomeworkTemplateMatchRes findActiveByCourseDetail(Long courseDetailId) {
        if (courseDetailId == null) {
            return null;
        }
        SysCourseDetail detail = courseDetailMapper.selectById(courseDetailId);
        if (detail == null) {
            return null;
        }
        SysCourse course = courseMapper.selectById(detail.getCourseId());
        if (course == null) {
            return null;
        }
        boolean online = TeachingModeEnum.ONLINE.name().equals(course.getTeachingMode());
        if ((online && StrUtil.isBlank(detail.getStageName()))
                || (!online && detail.getDayNumber() == null)) {
            return null;
        }
        CourseHomeworkTemplate template = templateMapper.selectOne(
                Wrappers.<CourseHomeworkTemplate>lambdaQuery()
                        .eq(CourseHomeworkTemplate::getCourseId, course.getId())
                        .eq(CourseHomeworkTemplate::getTeachingMode, course.getTeachingMode())
                        .eq(CourseHomeworkTemplate::getStatus, ActiveEnum.ACTIVE.name())
                        .eq(online, CourseHomeworkTemplate::getStageName, detail.getStageName())
                        .eq(!online, CourseHomeworkTemplate::getDayNumber, detail.getDayNumber())
                        .last("LIMIT 1"));
        return template == null ? null : new CourseHomeworkTemplateMatchRes(
                template.getId(), template.getContentFileName(), template.getStageName(), template.getDayNumber());
    }

    private CourseHomeworkTemplatePreviewRes toPreview(CourseHomeworkTemplate template) {
        OssDownloadUrlRes signed = ossService.generateDownloadUrl(
                template.getContentObjectKey(), true, template.getContentFileName());
        return new CourseHomeworkTemplatePreviewRes(
                template.getId(), template.getContentFileName(), signed.downloadUrl(),
                signed.expireSeconds(), template.getStageName(), template.getDayNumber());
    }

    private CourseHomeworkTemplatePositionRes toPosition(SysCourseDetail detail) {
        String label = detail.getDayNumber() == null
                ? detail.getStageName()
                : "D" + detail.getDayNumber() + " · " + StrUtil.blankToDefault(detail.getStageName(), detail.getClassContent());
        return new CourseHomeworkTemplatePositionRes(
                detail.getId(), detail.getStageName(), detail.getDayNumber(), detail.getClassContent(), label);
    }

    private TemplateBinding resolveBinding(Long courseId, Long courseDetailId) {
        SysCourse course = requireCourse(courseId);
        SysCourseDetail detail = courseDetailMapper.selectById(courseDetailId);
        if (detail == null || !courseId.equals(detail.getCourseId())) {
            throw BusinessException.COURSE_DETAIL_NOT_EXIST.newInstance("所选课程阶段/天次不存在或不属于该课程");
        }
        if (TeachingModeEnum.ONLINE.name().equals(course.getTeachingMode())) {
            if (StrUtil.isBlank(detail.getStageName())) {
                throw BusinessException.PARAMS_ERROR.newInstance("线上课程详情缺少阶段名称");
            }
        } else if (TeachingModeEnum.OFFLINE.name().equals(course.getTeachingMode())) {
            if (detail.getDayNumber() == null) {
                throw BusinessException.PARAMS_ERROR.newInstance("线下课程详情缺少天次");
            }
        } else {
            throw BusinessException.PARAMS_ERROR.newInstance("课程上课方式不正确");
        }
        return new TemplateBinding(course, detail);
    }

    private void applyBinding(CourseHomeworkTemplate entity, TemplateBinding binding) {
        boolean online = TeachingModeEnum.ONLINE.name().equals(binding.course().getTeachingMode());
        entity.setCourseId(binding.course().getId());
        entity.setTeachingMode(binding.course().getTeachingMode());
        entity.setStageName(online ? binding.detail().getStageName().trim() : null);
        entity.setDayNumber(online ? null : binding.detail().getDayNumber());
    }

    private void insertOrThrow(CourseHomeworkTemplate entity, TemplateBinding binding) {
        try {
            if (templateMapper.insert(entity) != 1) {
                throw BusinessException.DATA_ERROR.newInstance("作业标准保存失败");
            }
        } catch (DuplicateKeyException ex) {
            throw duplicateBindingException(binding);
        }
    }

    private BusinessException duplicateBindingException(TemplateBinding binding) {
        String position = TeachingModeEnum.ONLINE.name().equals(binding.course().getTeachingMode())
                ? binding.detail().getStageName()
                : "D" + binding.detail().getDayNumber();
        return BusinessException.DATA_EXIST.newInstance(binding.course().getCourseName() + "的" + position + "已配置作业标准");
    }

    private SysCourse requireCourse(Long courseId) {
        SysCourse course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.COURSE_NOT_EXIST.newInstance("课程不存在");
        }
        return course;
    }

    private CourseHomeworkTemplate requireTemplate(Long id) {
        CourseHomeworkTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw BusinessException.DATA_ERROR.newInstance("作业标准不存在");
        }
        return template;
    }

    private record TemplateBinding(SysCourse course, SysCourseDetail detail) {
    }
}
