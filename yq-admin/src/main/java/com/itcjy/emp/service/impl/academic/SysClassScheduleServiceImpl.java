package com.itcjy.emp.service.impl.academic;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.constants.ClassScheduleConstants;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.common.pojo.HolidayInfo;
import com.itcjy.common.utils.HolidayUtil;
import com.itcjy.emp.mapper.academic.SysClassMapper;
import com.itcjy.emp.mapper.academic.SysClassScheduleMapper;
import com.itcjy.emp.mapper.academic.SysCourseDetailMapper;
import com.itcjy.emp.mapper.system.SysRoleMapper;
import com.itcjy.emp.mapper.system.SysUserMapper;
import com.itcjy.emp.mapper.system.SysUserRoleMapper;
import com.itcjy.emp.pojo.entity.SysClass;
import com.itcjy.emp.pojo.entity.SysClassSchedule;
import com.itcjy.emp.pojo.entity.SysCourseDetail;
import com.itcjy.emp.pojo.entity.SysRole;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.entity.SysUserRole;
import com.itcjy.emp.pojo.req.academic.SysClassScheduleGenerateReq;
import com.itcjy.emp.pojo.req.academic.SysClassScheduleTeacherAssignReq;
import com.itcjy.emp.pojo.req.academic.SysClassScheduleTemporaryCourseReq;
import com.itcjy.emp.pojo.req.academic.SysClassScheduleUpdateReq;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleGenerateRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTeacherAssignRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTeacherAssignmentOptionsRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTemporaryCourseOptionsRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTemporaryCourseRes;
import com.itcjy.emp.service.academic.ISysClassScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysClassScheduleServiceImpl extends ServiceImpl<SysClassScheduleMapper, SysClassSchedule>
        implements ISysClassScheduleService {

    private static final int MAX_GENERATION_DAYS = 10_000;
    private static final String LECTURER_ROLE_CODE = "LECTURER";
    private static final String TEMPORARY_COURSE_CONTENT = "临时加课";

    private final SysClassMapper sysClassMapper;
    private final SysCourseDetailMapper sysCourseDetailMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
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
        Map<Long, SysUser> teacherMap = loadUserMap(schedules.stream()
                .map(SysClassSchedule::getTeacherId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());

        return schedules.stream()
                .map(schedule -> {
                    SysCourseDetail detail = schedule.getCourseDetailId() == null
                            ? null
                            : courseDetailMap.get(schedule.getCourseDetailId());
                    String stageName = detail == null ? null : detail.getStageName();
                    SysUser teacher = schedule.getTeacherId() == null
                            ? null
                            : teacherMap.get(schedule.getTeacherId());
                    String teacherName = teacher == null ? null : getUserDisplayName(teacher);
                    return SysClassScheduleRes.from(schedule, stageName, teacherName);
                })
                .toList();
    }

    @Override
    public SysClassScheduleRes getSchedule(Long scheduleId) {
        return toScheduleResponse(requireSchedule(scheduleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysClassScheduleRes updateSchedule(Long scheduleId, SysClassScheduleUpdateReq req) {
        SysClassSchedule currentSchedule = requireSchedule(scheduleId);
        requireTeachingSchedule(currentSchedule);
        SysClass lockedClass = sysClassMapper.selectByIdForUpdate(currentSchedule.getClassId());
        if (lockedClass == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }

        SysUser teacher = sysUserMapper.selectByIdForUpdate(req.teacherId());
        validateLecturer(teacher);
        SysClassSchedule lockedSchedule = baseMapper.selectByIdForUpdate(scheduleId);
        if (lockedSchedule == null) {
            throw BusinessException.CLASS_SCHEDULE_NOT_EXIST.newInstance("课程日程不存在");
        }
        requireTeachingSchedule(lockedSchedule);
        if (!Objects.equals(lockedSchedule.getClassId(), lockedClass.getId())) {
            throw BusinessException.TEMPORARY_COURSE_ERROR.newInstance("课程日程所属班级已发生变化，请刷新后重试");
        }

        String stageName = req.stageName().trim();
        SysCourseDetail currentDetail = sysCourseDetailMapper.selectById(lockedSchedule.getCourseDetailId());
        String currentStageName = currentDetail == null ? null : currentDetail.getStageName();
        if (!Objects.equals(currentStageName, stageName)) {
            SysCourseDetail stageAnchor = findStageAnchor(lockedClass.getCourseId(), stageName);
            lockedSchedule.setCourseDetailId(stageAnchor.getId());
        }

        validateTeacherAvailability(
                teacher.getId(),
                List.of(lockedSchedule.getScheduleDate()),
                List.of(lockedSchedule.getId()));
        lockedSchedule.setTeacherId(teacher.getId());
        lockedSchedule.setCourseContent(req.courseContent().trim());
        if (baseMapper.updateById(lockedSchedule) <= 0) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("课程日程修改失败");
        }
        return SysClassScheduleRes.from(lockedSchedule, stageName, getUserDisplayName(teacher));
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        SysClassSchedule currentSchedule = requireSchedule(scheduleId);
        requireTeachingSchedule(currentSchedule);
        HolidayInfo holidayInfo = getHolidayInfo(currentSchedule.getScheduleDate());
        List<SysClassSchedule> schedules = listClassSchedules(currentSchedule.getClassId());
        ScheduleDeletePlan plan = buildScheduleDeletePlan(currentSchedule, schedules, holidayInfo);
        transactionTemplate.executeWithoutResult(status -> executeScheduleDeletePlan(plan));
    }

    private ScheduleDeletePlan buildScheduleDeletePlan(
            SysClassSchedule targetSchedule,
            List<SysClassSchedule> schedules,
            HolidayInfo holidayInfo) {
        List<SysClassSchedule> updates = new ArrayList<>();
        List<Long> deleteIds = new ArrayList<>();
        List<TeacherDateAssignment> teacherAssignments = new ArrayList<>();

        if (isHoliday(holidayInfo)) {
            SysClassSchedule holidaySchedule = copySchedule(targetSchedule);
            holidaySchedule.setTeacherId(null);
            holidaySchedule.setCourseDetailId(null);
            holidaySchedule.setCourseContent(StrUtil.isNotBlank(holidayInfo.getName())
                    ? holidayInfo.getName()
                    : "法定节假日");
            holidaySchedule.setClassType(ClassScheduleConstants.DayType.HOLIDAY);
            updates.add(holidaySchedule);
            return new ScheduleDeletePlan(
                    targetSchedule.getClassId(),
                    snapshotSchedules(schedules),
                    updates,
                    deleteIds,
                    teacherAssignments);
        }

        List<SysClassSchedule> teachingSchedules = schedules.stream()
                .filter(schedule -> ClassScheduleConstants.DayType.CLASS.equals(schedule.getClassType()))
                .sorted(Comparator.comparing(SysClassSchedule::getScheduleDate)
                        .thenComparing(SysClassSchedule::getId))
                .toList();
        int targetIndex = -1;
        for (int index = 0; index < teachingSchedules.size(); index++) {
            if (Objects.equals(teachingSchedules.get(index).getId(), targetSchedule.getId())) {
                targetIndex = index;
                break;
            }
        }
        if (targetIndex < 0) {
            throw BusinessException.CLASS_SCHEDULE_NOT_EXIST.newInstance("课程日程不存在");
        }

        for (int index = targetIndex; index < teachingSchedules.size() - 1; index++) {
            SysClassSchedule destination = copySchedule(teachingSchedules.get(index));
            SysClassSchedule source = teachingSchedules.get(index + 1);
            copyCoursePayload(source, destination);
            updates.add(destination);
            addTeacherAssignment(
                    teacherAssignments,
                    source,
                    destination.getScheduleDate(),
                    destination.getId());
        }

        if (teachingSchedules.size() == 1) {
            deleteIds.addAll(schedules.stream().map(SysClassSchedule::getId).toList());
        } else {
            LocalDate newEndDate = teachingSchedules.get(teachingSchedules.size() - 2).getScheduleDate();
            deleteIds.addAll(schedules.stream()
                    .filter(schedule -> schedule.getScheduleDate().isAfter(newEndDate))
                    .map(SysClassSchedule::getId)
                    .toList());
        }
        return new ScheduleDeletePlan(
                targetSchedule.getClassId(),
                snapshotSchedules(schedules),
                updates,
                deleteIds,
                teacherAssignments);
    }

    private void executeScheduleDeletePlan(ScheduleDeletePlan plan) {
        if (sysClassMapper.selectByIdForUpdate(plan.classId()) == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }

        plan.teacherAssignments().stream()
                .map(TeacherDateAssignment::teacherId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(sysUserMapper::selectByIdForUpdate);
        plan.expectedSchedules().stream()
                .map(ScheduleSnapshot::id)
                .filter(Objects::nonNull)
                .sorted()
                .forEach(baseMapper::selectByIdForUpdate);

        List<SysClassSchedule> currentSchedules = listClassSchedules(plan.classId());
        if (!Objects.equals(plan.expectedSchedules(), snapshotSchedules(currentSchedules))) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("课表已发生变化，请刷新后重新删除");
        }
        validatePlannedTeacherAssignments(plan.teacherAssignments());
        if (!plan.updates().isEmpty() && !this.updateBatchById(plan.updates())) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("后续课程前移失败");
        }
        if (!plan.deleteIds().isEmpty() && !this.removeByIds(plan.deleteIds())) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("课程日程删除失败");
        }
    }

    @Override
    public SysClassScheduleTeacherAssignmentOptionsRes listTeacherAssignmentOptions(Long classId) {
        requireClass(classId);
        List<SysClassSchedule> classSchedules = listClassSchedules(classId);
        if (classSchedules.isEmpty()) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("该班级尚未生成课程表");
        }

        List<SysClassSchedule> teachingSchedules = classSchedules.stream()
                .filter(this::isTeachingSchedule)
                .toList();
        Map<Long, SysCourseDetail> courseDetailMap = loadCourseDetailMap(teachingSchedules);
        Map<String, List<SysClassSchedule>> stageScheduleMap = groupSchedulesByStage(teachingSchedules, courseDetailMap);
        if (stageScheduleMap.isEmpty()) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("该班级课程表暂无可分配教师的课程阶段");
        }

        List<SysUser> lecturers = listActiveLecturers();
        Set<LocalDate> currentClassDates = teachingSchedules.stream()
                .map(SysClassSchedule::getScheduleDate)
                .collect(Collectors.toSet());
        Map<Long, List<LocalDate>> occupiedDateMap = listOccupiedDates(
                lecturers.stream().map(SysUser::getId).toList(),
                currentClassDates,
                classId);

        Set<Long> assignedTeacherIds = teachingSchedules.stream()
                .map(SysClassSchedule::getTeacherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> assignedTeacherMap = loadUserMap(assignedTeacherIds);

        List<SysClassScheduleTeacherAssignmentOptionsRes.StageOption> stages = stageScheduleMap.entrySet().stream()
                .map(entry -> toStageOption(entry.getKey(), entry.getValue(), assignedTeacherMap))
                .toList();
        List<SysClassScheduleTeacherAssignmentOptionsRes.TeacherOption> teachers = lecturers.stream()
                .map(teacher -> new SysClassScheduleTeacherAssignmentOptionsRes.TeacherOption(
                        teacher.getId(),
                        getUserDisplayName(teacher),
                        occupiedDateMap.getOrDefault(teacher.getId(), List.of())))
                .toList();
        return new SysClassScheduleTeacherAssignmentOptionsRes(stages, teachers);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysClassScheduleTeacherAssignRes assignTeacherByStage(SysClassScheduleTeacherAssignReq req) {
        // 先锁班级，避免同一班级的教师分配、临时加课等写操作并发修改课表。
        SysClass sysClass = sysClassMapper.selectByIdForUpdate(req.classId());
        if (sysClass == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }
        String stageName = req.stageName().trim();
        List<SysClassSchedule> stageSchedules = listStageSchedules(sysClass, stageName);

        // 锁定教师行，使同一教师的并发分配串行执行。
        SysUser teacher = sysUserMapper.selectByIdForUpdate(req.teacherId());
        validateLecturer(teacher);

        List<LocalDate> classDates = stageSchedules.stream()
                .map(SysClassSchedule::getScheduleDate)
                .toList();
        List<Long> scheduleIds = stageSchedules.stream()
                .map(SysClassSchedule::getId)
                .toList();
        validateTeacherAvailability(teacher.getId(), classDates, scheduleIds);

        boolean updated = this.update(Wrappers.<SysClassSchedule>lambdaUpdate()
                .in(SysClassSchedule::getId, scheduleIds)
                .set(SysClassSchedule::getTeacherId, teacher.getId()));
        if (!updated) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("教师分配失败");
        }

        return new SysClassScheduleTeacherAssignRes(
                sysClass.getId(),
                stageName,
                teacher.getId(),
                getUserDisplayName(teacher),
                classDates.get(0),
                classDates.get(classDates.size() - 1),
                classDates.size());
    }

    @Override
    public SysClassScheduleTemporaryCourseOptionsRes listTemporaryCourseOptions(Long classId) {
        SysClass sysClass = requireClass(classId);
        List<SysCourseDetail> courseDetails = sysCourseDetailMapper.selectList(
                Wrappers.<SysCourseDetail>lambdaQuery()
                        .eq(SysCourseDetail::getCourseId, sysClass.getCourseId())
                        .orderByAsc(SysCourseDetail::getDayNumber)
                        .orderByAsc(SysCourseDetail::getId));
        List<SysClassScheduleTemporaryCourseOptionsRes.StageOption> stageOptions = courseDetails.stream()
                .filter(detail -> StrUtil.isNotBlank(detail.getStageName()))
                .collect(Collectors.groupingBy(
                        SysCourseDetail::getStageName,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> new SysClassScheduleTemporaryCourseOptionsRes.StageOption(
                        entry.getKey(),
                        entry.getKey() + "（" + entry.getValue().size() + "节课程）",
                        entry.getValue().size()))
                .toList();
        List<SysClassScheduleTemporaryCourseOptionsRes.TeacherOption> teacherOptions = listActiveLecturers().stream()
                .map(teacher -> new SysClassScheduleTemporaryCourseOptionsRes.TeacherOption(
                        teacher.getId(),
                        getUserDisplayName(teacher)))
                .toList();
        return new SysClassScheduleTemporaryCourseOptionsRes(stageOptions, teacherOptions);
    }

    @Override
    public SysClassScheduleTemporaryCourseRes addTemporaryCourse(SysClassScheduleTemporaryCourseReq req) {
        SysClass sysClass = requireClass(req.classId());
        String stageName = req.stageName().trim();
        SysCourseDetail courseDetail = findStageAnchor(sysClass.getCourseId(), stageName);
        SysUser teacher = sysUserMapper.selectById(req.teacherId());
        validateLecturer(teacher);

        List<SysClassSchedule> schedules = listClassSchedules(sysClass.getId());
        if (schedules.isEmpty()) {
            throw BusinessException.TEMPORARY_COURSE_ERROR.newInstance("该班级尚未生成课程表，无法临时加课");
        }

        TemporaryCoursePlan plan = buildTemporaryCoursePlan(
                sysClass,
                courseDetail,
                teacher,
                req.scheduleDate(),
                schedules);
        SysClassScheduleTemporaryCourseRes result = transactionTemplate.execute(
                status -> executeTemporaryCoursePlan(req, plan));
        if (result == null) {
            throw BusinessException.TEMPORARY_COURSE_ERROR.newInstance("临时加课失败");
        }
        return result;
    }

    private TemporaryCoursePlan buildTemporaryCoursePlan(
            SysClass sysClass,
            SysCourseDetail courseDetail,
            SysUser teacher,
            LocalDate scheduleDate,
            List<SysClassSchedule> schedules) {
        Map<LocalDate, SysClassSchedule> scheduleMap = schedules.stream()
                .collect(Collectors.toMap(
                        SysClassSchedule::getScheduleDate,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        SysClassSchedule targetSchedule = scheduleMap.get(scheduleDate);
        List<SysClassSchedule> updates = new ArrayList<>();
        List<SysClassSchedule> inserts = new ArrayList<>();
        List<TeacherDateAssignment> teacherAssignments = new ArrayList<>();
        teacherAssignments.add(new TeacherDateAssignment(
                teacher.getId(),
                scheduleDate,
                targetSchedule == null ? null : targetSchedule.getId()));

        boolean shifted = targetSchedule != null
                && ClassScheduleConstants.DayType.CLASS.equals(targetSchedule.getClassType());
        int shiftedClassCount = 0;

        if (!shifted) {
            if (targetSchedule == null) {
                inserts.add(createTemporaryCourseSchedule(
                        sysClass.getId(), scheduleDate, courseDetail, teacher.getId()));
            } else {
                SysClassSchedule targetUpdate = copySchedule(targetSchedule);
                fillTemporaryCourse(targetUpdate, courseDetail, teacher.getId());
                updates.add(targetUpdate);
            }
        } else {
            List<SysClassSchedule> shiftedSchedules = schedules.stream()
                    .filter(schedule -> ClassScheduleConstants.DayType.CLASS.equals(schedule.getClassType()))
                    .filter(schedule -> !schedule.getScheduleDate().isBefore(scheduleDate))
                    .sorted(Comparator.comparing(SysClassSchedule::getScheduleDate)
                            .thenComparing(SysClassSchedule::getId))
                    .toList();
            shiftedClassCount = shiftedSchedules.size();

            SysClassSchedule targetUpdate = copySchedule(shiftedSchedules.get(0));
            fillTemporaryCourse(targetUpdate, courseDetail, teacher.getId());
            updates.add(targetUpdate);

            for (int index = 1; index < shiftedSchedules.size(); index++) {
                SysClassSchedule source = shiftedSchedules.get(index - 1);
                SysClassSchedule destination = copySchedule(shiftedSchedules.get(index));
                copyCoursePayload(source, destination);
                updates.add(destination);
                addTeacherAssignment(teacherAssignments, source, destination.getScheduleDate(), destination.getId());
            }

            SysClassSchedule lastShiftedSchedule = shiftedSchedules.get(shiftedSchedules.size() - 1);
            ScheduleExtension extension = buildScheduleExtension(
                    sysClass,
                    lastShiftedSchedule.getScheduleDate(),
                    scheduleMap,
                    loadRule());
            inserts.addAll(extension.nonClassSchedules());
            SysClassSchedule finalSchedule = createSchedule(
                    sysClass,
                    extension.nextClassDate(),
                    null,
                    lastShiftedSchedule.getCourseContent(),
                    ClassScheduleConstants.DayType.CLASS);
            copyCoursePayload(lastShiftedSchedule, finalSchedule);
            inserts.add(finalSchedule);
            addTeacherAssignment(
                    teacherAssignments,
                    lastShiftedSchedule,
                    finalSchedule.getScheduleDate(),
                    null);
        }

        LocalDate endDate = schedules.stream()
                .map(SysClassSchedule::getScheduleDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(scheduleDate);
        for (SysClassSchedule insert : inserts) {
            if (insert.getScheduleDate().isAfter(endDate)) {
                endDate = insert.getScheduleDate();
            }
        }

        return new TemporaryCoursePlan(
                sysClass,
                courseDetail,
                scheduleDate,
                snapshotSchedules(schedules),
                updates,
                inserts,
                teacherAssignments,
                shifted,
                shiftedClassCount,
                endDate);
    }

    private SysClassScheduleTemporaryCourseRes executeTemporaryCoursePlan(
            SysClassScheduleTemporaryCourseReq req,
            TemporaryCoursePlan plan) {
        if (sysClassMapper.selectByIdForUpdate(req.classId()) == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }

        Map<Long, SysUser> lockedTeacherMap = new LinkedHashMap<>();
        plan.teacherAssignments().stream()
                .map(TeacherDateAssignment::teacherId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(teacherId -> lockedTeacherMap.put(
                        teacherId,
                        sysUserMapper.selectByIdForUpdate(teacherId)));
        SysUser temporaryTeacher = lockedTeacherMap.get(req.teacherId());
        validateLecturer(temporaryTeacher);

        List<SysClassSchedule> currentSchedules = listClassSchedules(req.classId());
        if (!Objects.equals(plan.expectedSchedules(), snapshotSchedules(currentSchedules))) {
            throw BusinessException.TEMPORARY_COURSE_ERROR.newInstance("课表已发生变化，请刷新后重新加课");
        }

        validatePlannedTeacherAssignments(plan.teacherAssignments());
        if (!plan.updates().isEmpty() && !this.updateBatchById(plan.updates())) {
            throw BusinessException.TEMPORARY_COURSE_ERROR.newInstance("临时课程调整失败");
        }
        if (!plan.inserts().isEmpty() && !this.saveBatch(plan.inserts())) {
            throw BusinessException.TEMPORARY_COURSE_ERROR.newInstance("临时课程保存失败");
        }

        return new SysClassScheduleTemporaryCourseRes(
                plan.sysClass().getId(),
                plan.scheduleDate(),
                plan.courseDetail().getStageName(),
                TEMPORARY_COURSE_CONTENT,
                temporaryTeacher.getId(),
                getUserDisplayName(temporaryTeacher),
                plan.shifted(),
                plan.shiftedClassCount(),
                plan.endDate());
    }

    private void validatePlannedTeacherAssignments(List<TeacherDateAssignment> assignments) {
        Map<Long, List<TeacherDateAssignment>> assignmentMap = assignments.stream()
                .filter(assignment -> assignment.teacherId() != null)
                .collect(Collectors.groupingBy(TeacherDateAssignment::teacherId));
        assignmentMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> validateTeacherAvailability(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(TeacherDateAssignment::scheduleDate)
                                .distinct()
                                .toList(),
                        entry.getValue().stream()
                                .map(TeacherDateAssignment::excludedScheduleId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList()));
    }

    private void validateTeacherAvailability(
            Long teacherId,
            Collection<LocalDate> classDates,
            Collection<Long> excludedScheduleIds) {
        if (teacherId == null || classDates == null || classDates.isEmpty()) {
            throw BusinessException.PARAMS_ERROR.newInstance("教师ID和上课日期不能为空");
        }
        List<Long> excludedIds = excludedScheduleIds == null
                ? List.of()
                : excludedScheduleIds.stream().filter(Objects::nonNull).distinct().toList();
        SysClassSchedule conflict = this.getOne(Wrappers.<SysClassSchedule>lambdaQuery()
                .eq(SysClassSchedule::getTeacherId, teacherId)
                .eq(SysClassSchedule::getClassType, ClassScheduleConstants.DayType.CLASS)
                .in(SysClassSchedule::getScheduleDate, classDates)
                .notIn(!excludedIds.isEmpty(), SysClassSchedule::getId, excludedIds)
                .orderByAsc(SysClassSchedule::getScheduleDate)
                .last("LIMIT 1"), false);
        if (conflict == null) {
            return;
        }
        SysClass conflictClass = sysClassMapper.selectById(conflict.getClassId());
        String className = conflictClass == null ? "其他班级" : "班级“" + conflictClass.getClassPeriod() + "”";
        throw BusinessException.TEACHER_SCHEDULE_CONFLICT.newInstance(
                "该教师在" + conflict.getScheduleDate() + "已为" + className + "授课，请选择其他教师");
    }

    private SysClass requireClass(Long classId) {
        SysClass sysClass = sysClassMapper.selectById(classId);
        if (sysClass == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("班级不存在");
        }
        return sysClass;
    }

    private SysClassSchedule requireSchedule(Long scheduleId) {
        SysClassSchedule schedule = baseMapper.selectById(scheduleId);
        if (schedule == null) {
            throw BusinessException.CLASS_SCHEDULE_NOT_EXIST.newInstance("课程日程不存在");
        }
        return schedule;
    }

    private void requireTeachingSchedule(SysClassSchedule schedule) {
        if (!ClassScheduleConstants.DayType.CLASS.equals(schedule.getClassType())) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("只有上课日程可以修改或删除");
        }
    }

    private SysCourseDetail findStageAnchor(Long courseId, String stageName) {
        List<SysCourseDetail> stageCourseDetails = sysCourseDetailMapper.selectList(
                Wrappers.<SysCourseDetail>lambdaQuery()
                        .eq(SysCourseDetail::getCourseId, courseId)
                        .eq(SysCourseDetail::getStageName, stageName)
                        .orderByAsc(SysCourseDetail::getDayNumber)
                        .orderByAsc(SysCourseDetail::getId));
        if (stageCourseDetails.isEmpty()) {
            throw BusinessException.COURSE_DETAIL_NOT_EXIST.newInstance("所选课程阶段不存在");
        }
        return stageCourseDetails.get(0);
    }

    private SysClassScheduleRes toScheduleResponse(SysClassSchedule schedule) {
        SysCourseDetail detail = schedule.getCourseDetailId() == null
                ? null
                : sysCourseDetailMapper.selectById(schedule.getCourseDetailId());
        SysUser teacher = schedule.getTeacherId() == null
                ? null
                : sysUserMapper.selectById(schedule.getTeacherId());
        return SysClassScheduleRes.from(
                schedule,
                detail == null ? null : detail.getStageName(),
                teacher == null ? null : getUserDisplayName(teacher));
    }

    private List<SysClassSchedule> listClassSchedules(Long classId) {
        return this.list(Wrappers.<SysClassSchedule>lambdaQuery()
                .eq(SysClassSchedule::getClassId, classId)
                .orderByAsc(SysClassSchedule::getScheduleDate)
                .orderByAsc(SysClassSchedule::getId));
    }

    private boolean isTeachingSchedule(SysClassSchedule schedule) {
        return ClassScheduleConstants.DayType.CLASS.equals(schedule.getClassType())
                && schedule.getCourseDetailId() != null;
    }

    private Map<Long, SysCourseDetail> loadCourseDetailMap(List<SysClassSchedule> schedules) {
        List<Long> detailIds = schedules.stream()
                .map(SysClassSchedule::getCourseDetailId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (detailIds.isEmpty()) {
            return Map.of();
        }
        return sysCourseDetailMapper.selectBatchIds(detailIds).stream()
                .collect(Collectors.toMap(SysCourseDetail::getId, Function.identity()));
    }

    private Map<String, List<SysClassSchedule>> groupSchedulesByStage(
            List<SysClassSchedule> schedules,
            Map<Long, SysCourseDetail> courseDetailMap) {
        return schedules.stream()
                .filter(schedule -> {
                    SysCourseDetail detail = courseDetailMap.get(schedule.getCourseDetailId());
                    return detail != null && StrUtil.isNotBlank(detail.getStageName());
                })
                .collect(Collectors.groupingBy(
                        schedule -> courseDetailMap.get(schedule.getCourseDetailId()).getStageName(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private List<SysUser> listActiveLecturers() {
        SysRole lecturerRole = findLecturerRole();
        List<Long> lecturerIds = sysUserRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery()
                                .eq(SysUserRole::getRoleId, lecturerRole.getId()))
                .stream()
                .map(SysUserRole::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (lecturerIds.isEmpty()) {
            return List.of();
        }
        return sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .in(SysUser::getId, lecturerIds)
                .eq(SysUser::getStatus, ActiveEnum.ACTIVE.name())
                .orderByAsc(SysUser::getId));
    }

    private Map<Long, List<LocalDate>> listOccupiedDates(
            List<Long> lecturerIds,
            Set<LocalDate> currentClassDates,
            Long currentClassId) {
        if (lecturerIds.isEmpty() || currentClassDates.isEmpty()) {
            return Map.of();
        }
        return this.list(Wrappers.<SysClassSchedule>lambdaQuery()
                        .in(SysClassSchedule::getTeacherId, lecturerIds)
                        .in(SysClassSchedule::getScheduleDate, currentClassDates)
                        .ne(SysClassSchedule::getClassId, currentClassId)
                        .eq(SysClassSchedule::getClassType, ClassScheduleConstants.DayType.CLASS)
                        .orderByAsc(SysClassSchedule::getScheduleDate))
                .stream()
                .collect(Collectors.groupingBy(
                        SysClassSchedule::getTeacherId,
                        Collectors.mapping(
                                SysClassSchedule::getScheduleDate,
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(HashSet::new),
                                        dates -> dates.stream().sorted().toList()))));
    }

    private SysClassScheduleTeacherAssignmentOptionsRes.StageOption toStageOption(
            String stageName,
            List<SysClassSchedule> schedules,
            Map<Long, SysUser> teacherMap) {
        List<LocalDate> classDates = schedules.stream()
                .map(SysClassSchedule::getScheduleDate)
                .distinct()
                .sorted()
                .toList();
        List<Long> teacherIds = schedules.stream()
                .map(SysClassSchedule::getTeacherId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        boolean hasUnassignedSchedule = schedules.stream()
                .anyMatch(schedule -> schedule.getTeacherId() == null);
        Long teacherId = teacherIds.size() == 1 && !hasUnassignedSchedule ? teacherIds.get(0) : null;
        String teacherName = null;
        if (hasUnassignedSchedule && !teacherIds.isEmpty()) {
            teacherName = "分配不完整";
        } else if (teacherIds.size() > 1) {
            teacherName = "多位教师";
        } else if (teacherId != null) {
            SysUser teacher = teacherMap.get(teacherId);
            teacherName = teacher == null ? "教师ID：" + teacherId : getUserDisplayName(teacher);
        }
        return new SysClassScheduleTeacherAssignmentOptionsRes.StageOption(
                stageName,
                classDates,
                teacherId,
                teacherName);
    }

    private List<SysClassSchedule> listStageSchedules(SysClass sysClass, String stageName) {
        List<Long> courseDetailIds = sysCourseDetailMapper.selectList(
                        Wrappers.<SysCourseDetail>lambdaQuery()
                                .eq(SysCourseDetail::getCourseId, sysClass.getCourseId())
                                .eq(SysCourseDetail::getStageName, stageName)
                                .orderByAsc(SysCourseDetail::getDayNumber)
                                .orderByAsc(SysCourseDetail::getId))
                .stream()
                .map(SysCourseDetail::getId)
                .toList();
        if (courseDetailIds.isEmpty()) {
            throw BusinessException.COURSE_DETAIL_NOT_EXIST.newInstance("所选课程阶段不存在");
        }
        List<SysClassSchedule> schedules = this.list(Wrappers.<SysClassSchedule>lambdaQuery()
                .eq(SysClassSchedule::getClassId, sysClass.getId())
                .eq(SysClassSchedule::getClassType, ClassScheduleConstants.DayType.CLASS)
                .in(SysClassSchedule::getCourseDetailId, courseDetailIds)
                .orderByAsc(SysClassSchedule::getScheduleDate)
                .orderByAsc(SysClassSchedule::getId));
        if (schedules.isEmpty()) {
            throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("所选阶段在当前班级课表中没有上课记录");
        }
        return schedules;
    }

    private void validateLecturer(SysUser teacher) {
        if (teacher == null) {
            throw BusinessException.USER_NOT_EXIST.newInstance("所选授课教师不存在");
        }
        if (!ActiveEnum.ACTIVE.name().equals(teacher.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("所选授课教师已停用");
        }
        SysRole lecturerRole = findLecturerRole();
        boolean isLecturer = sysUserRoleMapper.selectCount(
                Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, teacher.getId())
                        .eq(SysUserRole::getRoleId, lecturerRole.getId())) > 0;
        if (!isLecturer) {
            throw BusinessException.DATA_ERROR.newInstance("所选用户不是讲师角色");
        }
    }

    private SysRole findLecturerRole() {
        SysRole lecturerRole = sysRoleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, LECTURER_ROLE_CODE));
        if (lecturerRole == null) {
            throw BusinessException.ROLE_NOT_EXIST.newInstance("讲师角色LECTURER不存在");
        }
        return lecturerRole;
    }

    private Map<Long, SysUser> loadUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private String getUserDisplayName(SysUser user) {
        String name = StrUtil.isNotBlank(user.getRealName())
                ? user.getRealName()
                : StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername();
        if (StrUtil.isNotBlank(user.getUsername()) && !Objects.equals(name, user.getUsername())) {
            return name + "（" + user.getUsername() + "）";
        }
        return name;
    }

    private SysClassSchedule createTemporaryCourseSchedule(
            Long classId,
            LocalDate scheduleDate,
            SysCourseDetail courseDetail,
            Long teacherId) {
        SysClassSchedule schedule = new SysClassSchedule();
        schedule.setClassId(classId);
        schedule.setScheduleDate(scheduleDate);
        fillTemporaryCourse(schedule, courseDetail, teacherId);
        return schedule;
    }

    private void fillTemporaryCourse(
            SysClassSchedule schedule,
            SysCourseDetail courseDetail,
            Long teacherId) {
        schedule.setTeacherId(teacherId);
        schedule.setCourseDetailId(courseDetail.getId());
        schedule.setCourseContent(TEMPORARY_COURSE_CONTENT);
        schedule.setClassType(ClassScheduleConstants.DayType.CLASS);
    }

    private SysClassSchedule copySchedule(SysClassSchedule source) {
        SysClassSchedule target = new SysClassSchedule();
        target.setId(source.getId());
        target.setClassId(source.getClassId());
        target.setTeacherId(source.getTeacherId());
        target.setScheduleDate(source.getScheduleDate());
        target.setCourseDetailId(source.getCourseDetailId());
        target.setCourseContent(source.getCourseContent());
        target.setClassType(source.getClassType());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    private void copyCoursePayload(SysClassSchedule source, SysClassSchedule destination) {
        destination.setTeacherId(source.getTeacherId());
        destination.setCourseDetailId(source.getCourseDetailId());
        destination.setCourseContent(source.getCourseContent());
        destination.setClassType(ClassScheduleConstants.DayType.CLASS);
    }

    private void addTeacherAssignment(
            List<TeacherDateAssignment> assignments,
            SysClassSchedule source,
            LocalDate destinationDate,
            Long excludedScheduleId) {
        if (source.getTeacherId() != null) {
            assignments.add(new TeacherDateAssignment(
                    source.getTeacherId(),
                    destinationDate,
                    excludedScheduleId));
        }
    }

    private ScheduleExtension buildScheduleExtension(
            SysClass sysClass,
            LocalDate lastClassDate,
            Map<LocalDate, SysClassSchedule> existingScheduleMap,
            ScheduleRule rule) {
        List<SysClassSchedule> extensionSchedules = new ArrayList<>();
        LocalDate currentDate = lastClassDate.plusDays(1);

        for (int days = 0; days < MAX_GENERATION_DAYS; days++) {
            if (existingScheduleMap.containsKey(currentDate)) {
                currentDate = currentDate.plusDays(1);
                continue;
            }
            ResolvedScheduleDay resolvedDay = resolveScheduleDay(currentDate, rule);
            if (ClassScheduleConstants.DayType.CLASS.equals(resolvedDay.classType())) {
                return new ScheduleExtension(extensionSchedules, currentDate);
            }
            extensionSchedules.add(createSchedule(
                    sysClass,
                    currentDate,
                    null,
                    resolvedDay.courseContent(),
                    resolvedDay.classType()));
            currentDate = currentDate.plusDays(1);
        }
        throw BusinessException.TEMPORARY_COURSE_ERROR.newInstance("课表顺延跨度过大，请检查排课规则");
    }

    private ResolvedScheduleDay resolveScheduleDay(LocalDate date, ScheduleRule rule) {
        HolidayInfo holidayInfo = getHolidayInfo(date);
        int weekDay = date.getDayOfWeek().getValue();
        if (rule.holidayRest() && isHoliday(holidayInfo)) {
            String holidayName = StrUtil.isNotBlank(holidayInfo.getName())
                    ? holidayInfo.getName()
                    : "法定节假日";
            return new ResolvedScheduleDay(ClassScheduleConstants.DayType.HOLIDAY, holidayName);
        }
        if (rule.classDays().contains(weekDay)) {
            return new ResolvedScheduleDay(ClassScheduleConstants.DayType.CLASS, null);
        }
        if (rule.selfStudyDays().contains(weekDay)) {
            return new ResolvedScheduleDay(ClassScheduleConstants.DayType.SELF_STUDY, "自习");
        }
        if (rule.restDays().contains(weekDay)) {
            return new ResolvedScheduleDay(ClassScheduleConstants.DayType.REST, "休息");
        }
        throw BusinessException.CLASS_SCHEDULE_ERROR.newInstance("当前日期未匹配到排课规则");
    }

    private List<ScheduleSnapshot> snapshotSchedules(List<SysClassSchedule> schedules) {
        return schedules.stream()
                .map(schedule -> new ScheduleSnapshot(
                        schedule.getId(),
                        schedule.getClassId(),
                        schedule.getTeacherId(),
                        schedule.getScheduleDate(),
                        schedule.getCourseDetailId(),
                        schedule.getCourseContent(),
                        schedule.getClassType()))
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

    private record TemporaryCoursePlan(
            SysClass sysClass,
            SysCourseDetail courseDetail,
            LocalDate scheduleDate,
            List<ScheduleSnapshot> expectedSchedules,
            List<SysClassSchedule> updates,
            List<SysClassSchedule> inserts,
            List<TeacherDateAssignment> teacherAssignments,
            boolean shifted,
            int shiftedClassCount,
            LocalDate endDate) {
    }

    private record ScheduleDeletePlan(
            Long classId,
            List<ScheduleSnapshot> expectedSchedules,
            List<SysClassSchedule> updates,
            List<Long> deleteIds,
            List<TeacherDateAssignment> teacherAssignments) {
    }

    private record TeacherDateAssignment(
            Long teacherId,
            LocalDate scheduleDate,
            Long excludedScheduleId) {
    }

    private record ScheduleSnapshot(
            Long id,
            Long classId,
            Long teacherId,
            LocalDate scheduleDate,
            Long courseDetailId,
            String courseContent,
            String classType) {
    }

    private record ScheduleExtension(
            List<SysClassSchedule> nonClassSchedules,
            LocalDate nextClassDate) {
    }

    private record ResolvedScheduleDay(
            String classType,
            String courseContent) {
    }

    private record ScheduleRule(
            Set<Integer> classDays,
            Set<Integer> selfStudyDays,
            Set<Integer> restDays,
            boolean holidayRest) {
    }
}
