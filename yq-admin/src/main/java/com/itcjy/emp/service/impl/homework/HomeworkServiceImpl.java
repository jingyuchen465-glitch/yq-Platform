package com.itcjy.emp.service.impl.homework;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.constants.ClassScheduleConstants;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.myEnum.HomeworkDateType;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.academic.SysClassMapper;
import com.itcjy.emp.mapper.academic.SysClassScheduleMapper;
import com.itcjy.emp.mapper.homework.HomeworkMapper;
import com.itcjy.emp.pojo.entity.Homework;
import com.itcjy.emp.pojo.entity.SysClass;
import com.itcjy.emp.pojo.entity.SysClassSchedule;
import com.itcjy.emp.pojo.req.homework.HomeworkAnswerUploadReq;
import com.itcjy.emp.pojo.req.homework.HomeworkAnswerVisibilityReq;
import com.itcjy.emp.pojo.req.homework.HomeworkPrefillReq;
import com.itcjy.emp.pojo.req.homework.HomeworkPublishReq;
import com.itcjy.emp.pojo.req.homework.HomeworkStatusPageReq;
import com.itcjy.emp.pojo.res.homework.HomeworkClassOptionRes;
import com.itcjy.emp.pojo.res.homework.HomeworkClassStatusRes;
import com.itcjy.emp.pojo.res.homework.HomeworkPrefillRes;
import com.itcjy.emp.pojo.res.homework.HomeworkPublishRes;
import com.itcjy.emp.pojo.res.homework.HomeworkStatusItemRes;
import com.itcjy.emp.service.homework.IHomeworkService;
import com.itcjy.emp.service.homework.ICourseHomeworkTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeworkServiceImpl extends ServiceImpl<HomeworkMapper, Homework> implements IHomeworkService {

    private static final DateTimeFormatter TITLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    private final SysClassMapper sysClassMapper;
    private final SysClassScheduleMapper sysClassScheduleMapper;
    private final ICourseHomeworkTemplateService courseHomeworkTemplateService;

    /**
     * 列出可选的班级
     * @return
     */
    @Override
    public List<HomeworkClassOptionRes> listClassOptions() {
        return sysClassMapper.selectList(Wrappers.<SysClass>lambdaQuery()
                        .orderByDesc(SysClass::getId))
                .stream()
                .map(sysClass -> new HomeworkClassOptionRes(sysClass.getId(), sysClass.getClassPeriod()))
                .toList();
    }

    /**
     * 填充作业信息
     * @param req
     * @return
     */
    @Override
    public HomeworkPrefillRes prefill(HomeworkPrefillReq req) {
        LocalDate homeworkDate = normalizeDate(req.getHomeworkDate());
        HomeworkContext context = validatePublishContext(req.getClassId(), homeworkDate);

        return new HomeworkPrefillRes(
                context.sysClass().getId(),
                context.sysClass().getClassPeriod(),
                homeworkDate,
                buildDefaultTitle(context.sysClass(), homeworkDate),
                context.schedule().getCourseContent(),
                homeworkDate.atStartOfDay(),
                LocalDateTime.of(homeworkDate, LocalTime.of(23, 59, 59)),
                courseHomeworkTemplateService.findActiveByCourseDetail(
                        context.schedule().getCourseDetailId())
        );
    }

    /**
     * 发布作业
     * @param req
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomeworkPublishRes publish(HomeworkPublishReq req) {
        LocalDate homeworkDate = normalizeDate(req.homeworkDate());
        HomeworkContext context = validatePublishContext(req.classId(), homeworkDate);
        validateTimeRange(req.startTime(), req.deadline());

        Homework homework = new Homework();
        homework.setTitle(req.title().trim());
        homework.setContentObjectKey(req.contentObjectKey().trim());
        homework.setContentFileName(req.contentFileName().trim());
        homework.setAnswerStudentVisible(false);
        homework.setClassId(context.sysClass().getId());
        homework.setHomeworkDate(homeworkDate);
        homework.setClassContent(context.schedule().getCourseContent());
        homework.setStartTime(req.startTime());
        homework.setDeadline(req.deadline());
        homework.setRemark(StrUtil.trim(req.remark()));

        try {
            if (!save(homework)) {
                throw BusinessException.DATA_ERROR.newInstance("作业发布失败");
            }
        } catch (DuplicateKeyException ex) {
            throw duplicateHomeworkException(context.sysClass(), homeworkDate);
        }
        return HomeworkPublishRes.from(homework);
    }

    @Override
    public PageResult<HomeworkClassStatusRes> pageStatus(HomeworkStatusPageReq req) {
        IPage<SysClass> classPage = sysClassMapper.selectPage(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<SysClass>lambdaQuery()
                        .like(StrUtil.isNotBlank(req.getClassName()), SysClass::getClassPeriod, req.getClassName())
                        .orderByDesc(SysClass::getId)
        );
        if (classPage.getRecords().isEmpty()) {
            return new PageResult<>(classPage.getTotal(), Collections.emptyList());
        }

        List<Long> classIds = classPage.getRecords().stream()
                .map(SysClass::getId)
                .toList();
        List<Homework> homeworks = list(buildStatusQuery(req, classIds));
        Map<Long, List<Homework>> homeworkGroups = homeworks.stream()
                .collect(Collectors.groupingBy(Homework::getClassId));

        List<HomeworkClassStatusRes> records = classPage.getRecords().stream()
                .map(sysClass -> HomeworkClassStatusRes.from(
                        sysClass,
                        homeworkGroups.getOrDefault(sysClass.getId(), Collections.emptyList())
                ))
                .toList();
        return new PageResult<>(classPage.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomeworkStatusItemRes saveAnswer(Long homeworkId, HomeworkAnswerUploadReq req) {
        Homework homework = requireHomework(homeworkId);
        homework.setAnswerObjectKey(req.answerObjectKey().trim());
        homework.setAnswerFileName(req.answerFileName().trim());
        homework.setAnswerStudentVisible(false);
        if (!updateById(homework)) {
            throw BusinessException.DATA_ERROR.newInstance("标准答案保存失败");
        }
        return HomeworkStatusItemRes.from(homework);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomeworkStatusItemRes updateAnswerVisibility(
            Long homeworkId,
            HomeworkAnswerVisibilityReq req) {
        Homework homework = requireHomework(homeworkId);
        if (Boolean.TRUE.equals(req.studentVisible()) && StrUtil.isBlank(homework.getAnswerObjectKey())) {
            throw BusinessException.HOMEWORK_ANSWER_NOT_EXIST.newInstance("请先上传标准答案，再设置为学生可见");
        }
        homework.setAnswerStudentVisible(req.studentVisible());
        if (!updateById(homework)) {
            throw BusinessException.DATA_ERROR.newInstance("答案可见性更新失败");
        }
        return HomeworkStatusItemRes.from(homework);
    }

    private LambdaQueryWrapper<Homework> buildStatusQuery(
            HomeworkStatusPageReq req,
            List<Long> classIds) {
        LambdaQueryWrapper<Homework> query = Wrappers.<Homework>lambdaQuery()
                .in(Homework::getClassId, classIds);
        if (req.getDateType() == HomeworkDateType.HOMEWORK_DATE) {
            query.eq(Homework::getHomeworkDate, req.getQueryDate());
        } else {
            LocalDateTime dayStart = req.getQueryDate().atStartOfDay();
            query.ge(Homework::getDeadline, dayStart)
                    .lt(Homework::getDeadline, dayStart.plusDays(1));
        }
        return query.orderByAsc(Homework::getClassId)
                .orderByAsc(Homework::getDeadline)
                .orderByAsc(Homework::getId);
    }

    private Homework requireHomework(Long homeworkId) {
        Homework homework = getById(homeworkId);
        if (homework == null) {
            throw BusinessException.HOMEWORK_NOT_EXIST.newInstance("作业不存在");
        }
        return homework;
    }

    /**
     * 验证发布作业的上下文
     * @param classId
     * @param homeworkDate
     * @return
     */
    private HomeworkContext validatePublishContext(Long classId, LocalDate homeworkDate) {
        SysClass sysClass = sysClassMapper.selectById(classId);
        if (sysClass == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }
        if (hasPublishedHomework(classId, homeworkDate)) {
            throw duplicateHomeworkException(sysClass, homeworkDate);
        }

        SysClassSchedule schedule = sysClassScheduleMapper.selectOne(
                Wrappers.<SysClassSchedule>lambdaQuery()
                        .eq(SysClassSchedule::getClassId, classId)
                        .eq(SysClassSchedule::getScheduleDate, homeworkDate)
                        .eq(SysClassSchedule::getClassType, ClassScheduleConstants.DayType.CLASS)
                        .last("LIMIT 1")
        );
        if (schedule == null) {
            throw BusinessException.HOMEWORK_SCHEDULE_NOT_EXIST
                    .newInstance("所选班级当天没有上课课表，不能发布作业");
        }
        if (StrUtil.isBlank(schedule.getCourseContent())) {
            throw BusinessException.HOMEWORK_SCHEDULE_NOT_EXIST
                    .newInstance("当天课表缺少课程内容，不能发布作业");
        }
        return new HomeworkContext(sysClass, schedule);
    }

    /**
     * 验证指定班级在指定日期是否已经发布过作业
     * @param classId
     * @param homeworkDate
     * @return
     */
    private boolean hasPublishedHomework(Long classId, LocalDate homeworkDate) {
        return count(Wrappers.<Homework>lambdaQuery()
                .eq(Homework::getClassId, classId)
                .eq(Homework::getHomeworkDate, homeworkDate)) > 0;
    }

    /**
     * 验证作业时间范围
     * @param startTime
     * @param deadline
     */
    private void validateTimeRange(LocalDateTime startTime, LocalDateTime deadline) {
        if (deadline.isBefore(startTime)) {
            throw BusinessException.HOMEWORK_TIME_ERROR.newInstance("截止时间不能早于开始时间");
        }
    }

    /**
     * 构建作业重复异常
     * @param sysClass
     * @param homeworkDate
     * @return
     */
    private BusinessException duplicateHomeworkException(SysClass sysClass, LocalDate homeworkDate) {
        return BusinessException.HOMEWORK_EXIST.newInstance(
                sysClass.getClassPeriod() + "在" + homeworkDate.format(TITLE_DATE_FORMATTER) + "已发布作业"
        );
    }

    /**
     * 构建默认作业标题
     * @param sysClass
     * @param homeworkDate
     * @return
     */
    private String buildDefaultTitle(SysClass sysClass, LocalDate homeworkDate) {
        return sysClass.getClassPeriod() + " · " + homeworkDate.format(TITLE_DATE_FORMATTER) + "作业";
    }

    /**
     * 规范化日期
     * @param homeworkDate
     * @return
     */
    private LocalDate normalizeDate(LocalDate homeworkDate) {
        return homeworkDate.atStartOfDay().toLocalDate();
    }

    /**
     * 作业上下文
     */
    private record HomeworkContext(SysClass sysClass, SysClassSchedule schedule) {
    }
}
