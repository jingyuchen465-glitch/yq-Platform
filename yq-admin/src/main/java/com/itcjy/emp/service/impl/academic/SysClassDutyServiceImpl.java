package com.itcjy.emp.service.impl.academic;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.constants.TeacherDutyType;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.common.pojo.HolidayInfo;
import com.itcjy.common.utils.HolidayUtil;
import com.itcjy.emp.mapper.academic.SysCampusMapper;
import com.itcjy.emp.mapper.academic.SysClassDutyMapper;
import com.itcjy.emp.mapper.academic.SysClassScheduleMapper;
import com.itcjy.emp.mapper.system.SysRoleMapper;
import com.itcjy.emp.mapper.system.SysUserMapper;
import com.itcjy.emp.mapper.system.SysUserRoleMapper;
import com.itcjy.emp.pojo.entity.SysCampus;
import com.itcjy.emp.pojo.entity.SysClassDuty;
import com.itcjy.emp.pojo.entity.SysRole;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.entity.SysUserRole;
import com.itcjy.emp.pojo.projection.ClassTeachingPeriodRow;
import com.itcjy.emp.pojo.req.academic.SysClassDutyDailyReq;
import com.itcjy.emp.pojo.req.academic.SysClassDutySaveReq;
import com.itcjy.emp.pojo.res.academic.SysClassDutyDailyRes;
import com.itcjy.emp.pojo.res.academic.SysClassDutyOptionsRes;
import com.itcjy.emp.service.academic.ISysClassDutyService;
import com.itcjy.emp.service.system.ISysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysClassDutyServiceImpl extends ServiceImpl<SysClassDutyMapper, SysClassDuty>
        implements ISysClassDutyService {

    private static final String LECTURER_ROLE_CODE = "LECTURER";
    private final SysClassDutyMapper dutyMapper;
    private final SysClassScheduleMapper classScheduleMapper;
    private final SysCampusMapper campusMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;
    private final ISysConfigService sysConfigService;
    private final HolidayUtil holidayUtil;

    @Override
    public SysClassDutyOptionsRes listOptions() {
        List<SysClassDutyOptionsRes.CampusOption> campuses = campusMapper.selectList(
                        Wrappers.<SysCampus>lambdaQuery().orderByAsc(SysCampus::getCampusLocation))
                .stream()
                .map(campus -> new SysClassDutyOptionsRes.CampusOption(
                        campus.getId(), campus.getCampusLocation()))
                .toList();
        List<SysClassDutyOptionsRes.TeacherOption> teachers = listActiveLecturers().stream()
                .map(user -> new SysClassDutyOptionsRes.TeacherOption(
                        user.getId(), getUserDisplayName(user)))
                .toList();
        List<SysClassDutyOptionsRes.DutyTypeOption> dutyTypes = Arrays.stream(TeacherDutyType.values())
                .map(type -> new SysClassDutyOptionsRes.DutyTypeOption(
                        type.name(), type.getLabel(), type.getStartTimeText(),
                        type.getEndTimeText(), type.isClassDuty()))
                .toList();
        return new SysClassDutyOptionsRes(campuses, teachers, dutyTypes);
    }

    /**
     * 查询指定校区、指定日期的班级值班安排（含晚自习值班、自习值班、校区统一值班）
     *
     * @param req 包含校区ID和值班日期
     * @return 当日值班总览，包括校区值班、各班晚自习/自习值班行、未排课班级列表及当日有课教师ID
     */
    @Override
    public SysClassDutyDailyRes getDailyDuties(SysClassDutyDailyReq req) {
        // 1. 校验校区是否存在
        SysCampus campus = requireCampus(req.getCampusId());
        DutyMode dutyMode = resolveDutyMode(req.getDutyDate());
        if (dutyMode == DutyMode.NO_DUTY) {
            return new SysClassDutyDailyRes(
                    req.getDutyDate(), campus.getId(), campus.getCampusLocation(), dutyMode.name(),
                    List.of(), null, List.of(), List.of(), List.of());
        }
        boolean selfStudyDay = dutyMode == DutyMode.SELF_STUDY;

        // 2. 查询该校区所有班级的教学周期，筛选出当日处于教学周期内的班级
        List<ClassTeachingPeriodRow> periods = classScheduleMapper.selectClassTeachingPeriods(req.getCampusId());
        List<ClassTeachingPeriodRow> activeClasses = periods.stream()
                .filter(period -> period.isActiveOn(req.getDutyDate()))
                .toList();

        // 3. 收集尚未生成课表的班级，前端用于提示
        List<SysClassDutyDailyRes.UnscheduledClass> unscheduledClasses = periods.stream()
                .filter(period -> !period.hasSchedule())
                .map(period -> new SysClassDutyDailyRes.UnscheduledClass(
                        period.getClassId(), period.getClassName()))
                .toList();

        // 4. 查询当日该校区所有值班记录，并批量加载值班教师姓名
        List<SysClassDuty> duties = dutyMapper.selectDaily(req.getCampusId(), req.getDutyDate());
        Map<Long, String> teacherNames = loadTeacherNames(duties);

        // 5. 自习日与晚自习值班互斥，只组装当天允许的值班类型
        SysClassDutyDailyRes.DutyAssignment campusDuty = selfStudyDay
                ? null
                : duties.stream()
                        .filter(duty -> duty.getClassId() == null)
                        .filter(duty -> TeacherDutyType.EVENING_STUDY_CAMPUS.name().equals(duty.getDutyType()))
                        .findFirst()
                        .map(duty -> toAssignment(duty, teacherNames))
                        .orElse(null);
        List<SysClassDutyDailyRes.ClassDutyRow> eveningRows = selfStudyDay
                ? List.of()
                : buildClassRows(activeClasses, duties, teacherNames, TeacherDutyType.EVENING_STUDY_CLASS);
        List<SysClassDutyDailyRes.ClassDutyRow> selfStudyRows = selfStudyDay
                ? buildClassRows(activeClasses, duties, teacherNames, TeacherDutyType.SELF_STUDY_CLASS)
                : List.of();

        // 7. 查询当日有排课的教师ID（用于前端标识忙碌教师，避免重复安排）
        List<Long> busyTeacherIds = classScheduleMapper.selectTeachingTeacherIds(
                        req.getDutyDate(), req.getDutyDate()).stream()
                .distinct()
                .toList();

        // 8. 组装并返回当日值班总览响应
        return new SysClassDutyDailyRes(
                req.getDutyDate(), campus.getId(), campus.getCampusLocation(),
                dutyMode.name(),
                busyTeacherIds,
                campusDuty, eveningRows, selfStudyRows, unscheduledClasses);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDuty(SysClassDutySaveReq req) {
        requireCampus(req.campusId());
        TeacherDutyType dutyType = TeacherDutyType.fromCode(req.dutyType());
        validateDutyTypeForDate(req.dutyDate(), dutyType);
        validateDutyScope(req, dutyType);
        requireActiveLecturer(req.teacherId());

        SysClassDuty existing = dutyMapper.selectScopeForUpdate(
                req.campusId(), req.classId(), req.dutyDate(), dutyType.name());
        validateDutyConflict(req.teacherId(), req.dutyDate(), dutyType,
                existing == null ? null : existing.getId());

        SysClassDuty duty = existing == null ? new SysClassDuty() : existing;
        duty.setCampusId(req.campusId());
        duty.setClassId(req.classId());
        duty.setTeacherId(req.teacherId());
        duty.setDutyDate(req.dutyDate());
        duty.setDutyType(dutyType.name());
        duty.setStartTime(dutyType.getStartTimeText());
        duty.setEndTime(dutyType.getEndTimeText());
        duty.setRemark(normalizeRemark(req.remark()));
        duty.setUpdatedAt(LocalDateTime.now());
        if (existing == null) {
            duty.setCreatedAt(LocalDateTime.now());
            dutyMapper.insert(duty);
        } else {
            dutyMapper.updateById(duty);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDuty(Long id) {
        SysClassDuty duty = dutyMapper.selectById(id);
        if (duty == null) {
            throw BusinessException.DUTY_NOT_EXIST.newInstance("值班安排不存在或已被清空");
        }
        dutyMapper.deleteById(id);
    }

    private List<SysClassDutyDailyRes.ClassDutyRow> buildClassRows(
            List<ClassTeachingPeriodRow> activeClasses,
            List<SysClassDuty> duties,
            Map<Long, String> teacherNames,
            TeacherDutyType dutyType) {
        Map<Long, SysClassDuty> dutyByClass = duties.stream()
                .filter(duty -> duty.getClassId() != null)
                .filter(duty -> dutyType.name().equals(duty.getDutyType()))
                .collect(Collectors.toMap(
                        SysClassDuty::getClassId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        return activeClasses.stream()
                .map(period -> new SysClassDutyDailyRes.ClassDutyRow(
                        period.getClassId(),
                        period.getClassName(),
                        period.getTeachingStartDate(),
                        period.getTeachingEndDate(),
                        dutyByClass.containsKey(period.getClassId())
                                ? toAssignment(dutyByClass.get(period.getClassId()), teacherNames)
                                : null))
                .toList();
    }

    private SysClassDutyDailyRes.DutyAssignment toAssignment(
            SysClassDuty duty, Map<Long, String> teacherNames) {
        TeacherDutyType type = TeacherDutyType.fromCode(duty.getDutyType());
        return new SysClassDutyDailyRes.DutyAssignment(
                duty.getId(), type.name(), type.getLabel(),
                type.getStartTimeText(), type.getEndTimeText(),
                duty.getTeacherId(), teacherNames.get(duty.getTeacherId()), duty.getRemark());
    }

    private Map<Long, String> loadTeacherNames(List<SysClassDuty> duties) {
        List<Long> teacherIds = duties.stream()
                .map(SysClassDuty::getTeacherId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (teacherIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(teacherIds).stream()
                .collect(Collectors.toMap(SysUser::getId, this::getUserDisplayName));
    }

    private void validateDutyScope(SysClassDutySaveReq req, TeacherDutyType dutyType) {
        if (!dutyType.isClassDuty()) {
            if (req.classId() != null) {
                throw BusinessException.PARAMS_ERROR.newInstance("校区统一值班不能指定班级");
            }
            return;
        }
        if (req.classId() == null) {
            throw BusinessException.PARAMS_ERROR.newInstance("班级值班必须指定班级");
        }
        ClassTeachingPeriodRow period = classScheduleMapper.selectClassTeachingPeriod(req.classId());
        if (period == null) {
            throw BusinessException.CLAZZ_NOT_EXIST.newInstance("所选班级不存在");
        }
        if (!Objects.equals(period.getCampusId(), req.campusId())) {
            throw BusinessException.DATA_ERROR.newInstance("所选班级不属于当前校区");
        }
        if (!period.hasSchedule()) {
            throw BusinessException.CLASS_SCHEDULE_NOT_EXIST.newInstance("所选班级尚未生成课表");
        }
        if (!period.isActiveOn(req.dutyDate())) {
            throw BusinessException.DATA_ERROR.newInstance("所选日期不在班级教学周期内");
        }
    }

    private void validateDutyConflict(Long teacherId, LocalDate dutyDate,
                                      TeacherDutyType dutyType, Long currentDutyId) {
        boolean conflict = dutyMapper.selectTeacherDuties(teacherId, dutyDate).stream()
                .filter(duty -> !Objects.equals(duty.getId(), currentDutyId))
                .map(duty -> TeacherDutyType.fromCode(duty.getDutyType()))
                .anyMatch(dutyType::overlaps);
        if (conflict) {
            throw BusinessException.DUTY_CONFLICT.newInstance("该老师在当前时段已有其他值班安排");
        }
    }

    private void validateDutyTypeForDate(LocalDate dutyDate, TeacherDutyType dutyType) {
        DutyMode dutyMode = resolveDutyMode(dutyDate);
        if (dutyMode == DutyMode.NO_DUTY) {
            throw BusinessException.DATA_ERROR.newInstance("节假日或休息日不能安排值班");
        }
        boolean selfStudyDay = dutyMode == DutyMode.SELF_STUDY;
        if (selfStudyDay && dutyType != TeacherDutyType.SELF_STUDY_CLASS) {
            throw BusinessException.DATA_ERROR.newInstance("自习日不能安排晚自习值班");
        }
        if (!selfStudyDay && dutyType == TeacherDutyType.SELF_STUDY_CLASS) {
            throw BusinessException.DATA_ERROR.newInstance("非自习日不能安排自习日值班");
        }
    }

    private DutyMode resolveDutyMode(LocalDate dutyDate) {
        int dayOfWeek = dutyDate.getDayOfWeek().getValue();
        var scheduleRule = sysConfigService.getClassScheduleRule();
        HolidayInfo holidayInfo = holidayUtil.getHolidayInfo(dutyDate);
        if ((holidayInfo != null && Boolean.TRUE.equals(holidayInfo.getHoliday()))
                || scheduleRule.restDays().contains(dayOfWeek)) {
            return DutyMode.NO_DUTY;
        }
        if (scheduleRule.selfStudyDays().contains(dayOfWeek)) {
            return DutyMode.SELF_STUDY;
        }
        return DutyMode.EVENING_STUDY;
    }

    private enum DutyMode {
        NO_DUTY,
        SELF_STUDY,
        EVENING_STUDY
    }

    private SysCampus requireCampus(Long campusId) {
        SysCampus campus = campusMapper.selectById(campusId);
        if (campus == null) {
            throw BusinessException.CAMPUS_NOT_EXIST.newInstance("所选校区不存在");
        }
        return campus;
    }

    private SysUser requireActiveLecturer(Long teacherId) {
        SysRole lecturerRole = roleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, LECTURER_ROLE_CODE));
        if (lecturerRole == null) {
            throw BusinessException.ROLE_NOT_EXIST.newInstance("讲师角色LECTURER不存在");
        }
        SysUser user = userMapper.selectById(teacherId);
        boolean hasRole = userRoleMapper.selectCount(
                Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, teacherId)
                        .eq(SysUserRole::getRoleId, lecturerRole.getId())) > 0;
        if (user == null || !ActiveEnum.ACTIVE.name().equals(user.getStatus()) || !hasRole) {
            throw BusinessException.DATA_ERROR.newInstance("所选用户不是有效讲师");
        }
        return user;
    }

    private List<SysUser> listActiveLecturers() {
        SysRole lecturerRole = roleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, LECTURER_ROLE_CODE));
        if (lecturerRole == null) {
            return Collections.emptyList();
        }
        List<Long> userIds = userRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery()
                                .eq(SysUserRole::getRoleId, lecturerRole.getId()))
                .stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectList(
                Wrappers.<SysUser>lambdaQuery()
                        .in(SysUser::getId, userIds)
                        .eq(SysUser::getStatus, ActiveEnum.ACTIVE.name())
                        .orderByAsc(SysUser::getId));
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

    private String normalizeRemark(String remark) {
        return StrUtil.isBlank(remark) ? null : remark.trim();
    }
}
