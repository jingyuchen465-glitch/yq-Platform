package com.itcjy.emp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.constants.ClassScheduleConstants;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.HolidayInfo;
import com.itcjy.common.utils.HolidayUtil;
import com.itcjy.emp.mapper.SysClassMapper;
import com.itcjy.emp.mapper.SysClassScheduleMapper;
import com.itcjy.emp.mapper.SysCourseDetailMapper;
import com.itcjy.emp.pojo.entity.SysClass;
import com.itcjy.emp.pojo.entity.SysClassSchedule;
import com.itcjy.emp.pojo.entity.SysCourseDetail;
import com.itcjy.emp.pojo.req.SysClassScheduleGenerateReq;
import com.itcjy.emp.pojo.res.SysClassScheduleGenerateRes;
import com.itcjy.emp.pojo.res.SysClassScheduleRes;
import com.itcjy.emp.service.ISysClassScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysClassScheduleServiceImpl extends ServiceImpl<SysClassScheduleMapper, SysClassSchedule>
        implements ISysClassScheduleService {

    private static final int MAX_GENERATION_DAYS = 10_000;

    private final SysClassMapper sysClassMapper;
    private final SysCourseDetailMapper sysCourseDetailMapper;
    private final HolidayUtil holidayUtil;
    private final TransactionTemplate transactionTemplate;

    @Override
    public SysClassScheduleGenerateRes generateSchedule(SysClassScheduleGenerateReq req) {
        SysClass sysClass = sysClassMapper.selectById(req.classId());
        if (sysClass == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }

        ScheduleRule rule = loadRule();
        HolidayInfo firstDayHoliday = getHolidayInfo(req.startDate());
        if (isHoliday(firstDayHoliday)) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("第一天上课日期不能是法定节假日");
        }
        if (!rule.classDays().contains(req.startDate().getDayOfWeek().getValue())) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("第一天上课日期不在允许的上课日中");
        }

        List<SysCourseDetail> courseDetails = sysCourseDetailMapper.selectList(
                Wrappers.<SysCourseDetail>lambdaQuery()
                        .eq(SysCourseDetail::getCourseId, sysClass.getCourseId())
                        .orderByAsc(SysCourseDetail::getDayNumber)
                        .orderByAsc(SysCourseDetail::getId)
        );
        if (courseDetails.isEmpty()) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("该班级关联课程暂无课程详情，无法生成课表");
        }

        List<SysClassSchedule> schedules = buildSchedules(sysClass, req.startDate(), courseDetails, rule);
        replaceSchedules(sysClass.getId(), schedules);

        LocalDate endDate = schedules.get(schedules.size() - 1).getScheduleDate();
        return new SysClassScheduleGenerateRes(
                sysClass.getId(),
                sysClass.getClassPeriod(),
                req.startDate(),
                endDate,
                schedules.size(),
                courseDetails.size()
        );
    }

    private void replaceSchedules(Long classId, List<SysClassSchedule> schedules) {
        transactionTemplate.executeWithoutResult(status -> {
            this.remove(Wrappers.<SysClassSchedule>lambdaQuery()
                    .eq(SysClassSchedule::getClassId, classId));
            if (!this.saveBatch(schedules)) {
                throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("课程表保存失败");
            }
        });
    }

    @Override
    public List<SysClassScheduleRes> listSchedule(Long classId) {
        if (sysClassMapper.selectById(classId) == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }
        List<SysClassSchedule> schedules = this.list(Wrappers.<SysClassSchedule>lambdaQuery()
                .eq(SysClassSchedule::getClassId, classId)
                .orderByAsc(SysClassSchedule::getScheduleDate));
        List<Long> courseDetailIds = schedules.stream()
                .map(SysClassSchedule::getCourseDetailId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, SysCourseDetail> courseDetailMap = courseDetailIds.isEmpty()
                ? Map.of()
                : sysCourseDetailMapper.selectBatchIds(courseDetailIds).stream()
                .collect(Collectors.toMap(SysCourseDetail::getId, Function.identity()));

        return schedules.stream()
                .map(schedule -> {
                    SysCourseDetail detail = courseDetailMap.get(schedule.getCourseDetailId());
                    String stageName = detail == null ? null : detail.getStageName();
                    return SysClassScheduleRes.from(schedule, stageName);
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByClassId(Long classId) {
        this.remove(Wrappers.<SysClassSchedule>lambdaQuery()
                .eq(SysClassSchedule::getClassId, classId));
    }

    private List<SysClassSchedule> buildSchedules(
            SysClass sysClass,
            LocalDate startDate,
            List<SysCourseDetail> courseDetails,
            ScheduleRule rule) {
        List<SysClassSchedule> schedules = new ArrayList<>();
        LocalDate currentDate = startDate;
        int detailIndex = 0;

        while (detailIndex < courseDetails.size()) {
            if (schedules.size() >= MAX_GENERATION_DAYS) {
                throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("排课跨度过大，请检查排课规则");
            }

            HolidayInfo holidayInfo = getHolidayInfo(currentDate);
            int weekDay = currentDate.getDayOfWeek().getValue();
            SysCourseDetail courseDetail = null;
            String classType;
            String courseContent;

            if (rule.holidayRest() && isHoliday(holidayInfo)) {
                classType = ClassScheduleConstants.DayType.HOLIDAY;
                courseContent = StrUtil.isNotBlank(holidayInfo.getName()) ? holidayInfo.getName() : "法定节假日";
            } else if (rule.classDays().contains(weekDay)) {
                classType = ClassScheduleConstants.DayType.CLASS;
                courseDetail = courseDetails.get(detailIndex++);
                courseContent = getCourseContent(courseDetail);
            } else if (rule.selfStudyDays().contains(weekDay)) {
                classType = ClassScheduleConstants.DayType.SELF_STUDY;
                courseContent = "自习";
            } else if (rule.restDays().contains(weekDay)) {
                classType = ClassScheduleConstants.DayType.REST;
                courseContent = "休息";
            } else {
                throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("当前日期未匹配到排课规则");
            }

            schedules.add(createSchedule(sysClass, currentDate, courseDetail, courseContent, classType));
            currentDate = currentDate.plusDays(1);
        }
        return schedules;
    }

    private SysClassSchedule createSchedule(
            SysClass sysClass,
            LocalDate scheduleDate,
            SysCourseDetail courseDetail,
            String courseContent,
            String classType) {
        SysClassSchedule schedule = new SysClassSchedule();
        schedule.setClassId(sysClass.getId());
        schedule.setTeacherId(sysClass.getHeadTeacherId());
        schedule.setScheduleDate(scheduleDate);
        schedule.setCourseDetailId(courseDetail == null ? null : courseDetail.getId());
        schedule.setCourseContent(courseContent);
        schedule.setClassType(classType);
        return schedule;
    }

    private String getCourseContent(SysCourseDetail detail) {
        if (StrUtil.isNotBlank(detail.getClassContent())) {
            return detail.getClassContent();
        }
        if (StrUtil.isNotBlank(detail.getStageName())) {
            return detail.getStageName();
        }
        return "课程第" + detail.getDayNumber() + "天";
    }

    private ScheduleRule loadRule() {
        JSONObject config = JSON.parseObject(ClassScheduleConstants.RULE_CONFIG);
        if (config == null) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("排课规则配置不存在");
        }
        Set<Integer> classDays = readDaySet(config, ClassScheduleConstants.RuleField.CLASS_DAYS);
        Set<Integer> selfStudyDays = readDaySet(config, ClassScheduleConstants.RuleField.SELF_STUDY_DAYS);
        Set<Integer> restDays = readDaySet(config, ClassScheduleConstants.RuleField.REST_DAYS);
        Boolean holidayRest = config.getBoolean(ClassScheduleConstants.RuleField.HOLIDAY_REST);
        validateRule(classDays, selfStudyDays, restDays, holidayRest);
        return new ScheduleRule(classDays, selfStudyDays, restDays, holidayRest);
    }

    private Set<Integer> readDaySet(JSONObject config, String fieldName) {
        List<Integer> days = config.getList(fieldName, Integer.class);
        return days == null ? Set.of() : Set.copyOf(days);
    }

    private void validateRule(
            Set<Integer> classDays,
            Set<Integer> selfStudyDays,
            Set<Integer> restDays,
            Boolean holidayRest) {
        if (classDays.isEmpty() || holidayRest == null) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("排课规则配置不完整");
        }
        Set<Integer> allDays = new HashSet<>();
        allDays.addAll(classDays);
        allDays.addAll(selfStudyDays);
        allDays.addAll(restDays);
        boolean validRange = allDays.stream().allMatch(day -> day >= 1 && day <= 7);
        int configuredCount = classDays.size() + selfStudyDays.size() + restDays.size();
        if (!validRange || allDays.size() != configuredCount || allDays.size() != 7) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("排课规则中的星期配置无效或存在重复");
        }
    }

    private HolidayInfo getHolidayInfo(LocalDate date) {
        return holidayUtil.getHolidayInfo(date);
    }

    private boolean isHoliday(HolidayInfo holidayInfo) {
        return holidayInfo != null && Boolean.TRUE.equals(holidayInfo.getHoliday());
    }

    private record ScheduleRule(
            Set<Integer> classDays,
            Set<Integer> selfStudyDays,
            Set<Integer> restDays,
            boolean holidayRest) {
    }
}
