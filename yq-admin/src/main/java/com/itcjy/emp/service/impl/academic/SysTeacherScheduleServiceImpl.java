package com.itcjy.emp.service.impl.academic;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.emp.mapper.academic.SysClassScheduleMapper;
import com.itcjy.emp.mapper.system.SysRoleMapper;
import com.itcjy.emp.mapper.system.SysUserMapper;
import com.itcjy.emp.mapper.system.SysUserRoleMapper;
import com.itcjy.emp.pojo.entity.SysRole;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.entity.SysUserRole;
import com.itcjy.emp.pojo.projection.TeacherScheduleRow;
import com.itcjy.emp.pojo.req.academic.SysTeacherScheduleCalendarReq;
import com.itcjy.emp.pojo.res.academic.SysTeacherScheduleCalendarRes;
import com.itcjy.emp.service.academic.ISysTeacherScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SysTeacherScheduleServiceImpl implements ISysTeacherScheduleService {

    private static final String LECTURER_ROLE_CODE = "LECTURER";

    private final SysClassScheduleMapper classScheduleMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;

    @Override
    public SysTeacherScheduleCalendarRes getTeacherCalendar(SysTeacherScheduleCalendarReq req) {
        //查询正在活跃的讲师列表
        List<SysUser> lecturers = listActiveLecturers();
        //解析查询启始时间
        YearMonth month = req.getMonth();
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<SysTeacherScheduleCalendarRes.TeacherOption> teacherOptions = lecturers.stream()
                .map(this::toTeacherOption)
                .toList();
        Set<Long> teachingTeacherIds = new HashSet<>(
                classScheduleMapper.selectTeachingTeacherIds(startDate, endDate));
        List<SysTeacherScheduleCalendarRes.TeacherOption> noCourseTeachers = lecturers.stream()
                .filter(teacher -> !teachingTeacherIds.contains(teacher.getId()))
                .map(this::toTeacherOption)
                .toList();

        if (lecturers.isEmpty()) {
            return new SysTeacherScheduleCalendarRes(
                    month, null, null, 0, List.of(), List.of(), List.of());
        }

        SysUser selectedTeacher = resolveSelectedTeacher(lecturers, req.getTeacherId());
        String teacherName = getUserDisplayName(selectedTeacher);
        List<TeacherScheduleRow> rows = classScheduleMapper.selectTeacherScheduleRows(
                selectedTeacher.getId(), startDate, endDate);
        List<SysTeacherScheduleCalendarRes.ScheduleItem> schedules = rows.stream()
                .map(row -> toScheduleItem(row, selectedTeacher.getId(), teacherName))
                .toList();
        int teachingDays = (int) schedules.stream()
                .map(SysTeacherScheduleCalendarRes.ScheduleItem::scheduleDate)
                .distinct()
                .count();

        return new SysTeacherScheduleCalendarRes(
                month,
                selectedTeacher.getId(),
                teacherName,
                teachingDays,
                teacherOptions,
                schedules,
                noCourseTeachers);
    }

    private List<SysUser> listActiveLecturers() {
        SysRole lecturerRole = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, LECTURER_ROLE_CODE));
        if (lecturerRole == null) {
            throw BusinessException.ROLE_NOT_EXIST.newInstance("讲师角色LECTURER不存在");
        }
        List<Long> lecturerIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getRoleId, lecturerRole.getId()))
                .stream()
                .map(SysUserRole::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (lecturerIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .in(SysUser::getId, lecturerIds)
                .eq(SysUser::getStatus, ActiveEnum.ACTIVE.name())
                .orderByAsc(SysUser::getRealName)
                .orderByAsc(SysUser::getId));
    }

    private SysUser resolveSelectedTeacher(List<SysUser> lecturers, Long teacherId) {
        if (teacherId == null) {
            return lecturers.get(0);
        }
        return lecturers.stream()
                .filter(teacher -> teacher.getId().equals(teacherId))
                .findFirst()
                .orElseThrow(() -> BusinessException.USER_NOT_EXIST.newInstance("所选教师不存在或不是有效讲师"));
    }

    private SysTeacherScheduleCalendarRes.TeacherOption toTeacherOption(SysUser teacher) {
        return new SysTeacherScheduleCalendarRes.TeacherOption(
                teacher.getId(), getUserDisplayName(teacher));
    }

    private SysTeacherScheduleCalendarRes.ScheduleItem toScheduleItem(
            TeacherScheduleRow row,
            Long teacherId,
            String teacherName) {
        return new SysTeacherScheduleCalendarRes.ScheduleItem(
                row.getScheduleId(),
                row.getScheduleDate(),
                row.getClassId(),
                row.getClassName(),
                teacherId,
                teacherName,
                row.getCourseDetailId(),
                row.getStageName(),
                row.getCourseContent());
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
}
