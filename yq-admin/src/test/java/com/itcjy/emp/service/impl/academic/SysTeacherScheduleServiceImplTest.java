package com.itcjy.emp.service.impl.academic;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysTeacherScheduleServiceImplTest {

    @Mock
    private SysClassScheduleMapper classScheduleMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysUserMapper userMapper;

    private SysTeacherScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        initTableInfo(SysRole.class);
        initTableInfo(SysUserRole.class);
        initTableInfo(SysUser.class);
        service = new SysTeacherScheduleServiceImpl(
                classScheduleMapper, roleMapper, userRoleMapper, userMapper);
    }

    @Test
    void shouldReturnTeacherCalendarAndNoCourseTeachers() {
        SysUser teacher = teacher(11L, "张明", "zhangming");
        SysUser idleTeacher = teacher(12L, "李青", "liqing");
        mockLecturers(List.of(teacher, idleTeacher));
        when(classScheduleMapper.selectTeachingTeacherIds(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(List.of(11L));
        when(classScheduleMapper.selectTeacherScheduleRows(
                11L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(List.of(
                        row(101L, LocalDate.of(2026, 7, 6), 21L, "Java第18期", "Java基础"),
                        row(102L, LocalDate.of(2026, 7, 7), 21L, "Java第18期", "面向对象")));

        SysTeacherScheduleCalendarRes result = service.getTeacherCalendar(request(11L));

        assertThat(result.teacherId()).isEqualTo(11L);
        assertThat(result.teacherName()).isEqualTo("张明（zhangming）");
        assertThat(result.teachingDays()).isEqualTo(2);
        assertThat(result.schedules()).extracting(SysTeacherScheduleCalendarRes.ScheduleItem::className)
                .containsOnly("Java第18期");
        assertThat(result.schedules()).extracting(SysTeacherScheduleCalendarRes.ScheduleItem::teacherName)
                .containsOnly("张明（zhangming）");
        assertThat(result.noCourseTeachers()).singleElement()
                .satisfies(option -> assertThat(option.name()).isEqualTo("李青（liqing）"));
    }

    @Test
    void shouldDefaultToFirstActiveLecturer() {
        SysUser teacher = teacher(11L, "张明", "zhangming");
        mockLecturers(List.of(teacher));
        when(classScheduleMapper.selectTeachingTeacherIds(any(), any())).thenReturn(List.of());
        when(classScheduleMapper.selectTeacherScheduleRows(11L, LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31))).thenReturn(List.of());

        SysTeacherScheduleCalendarRes result = service.getTeacherCalendar(request(null));

        assertThat(result.teacherId()).isEqualTo(11L);
        assertThat(result.schedules()).isEmpty();
        assertThat(result.noCourseTeachers()).hasSize(1);
    }

    @Test
    void shouldRejectTeacherOutsideActiveLecturerOptions() {
        mockLecturers(List.of(teacher(11L, "张明", "zhangming")));
        when(classScheduleMapper.selectTeachingTeacherIds(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.getTeacherCalendar(request(99L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不是有效讲师");
        verify(classScheduleMapper, never()).selectTeacherScheduleRows(any(), any(), any());
    }

    private void mockLecturers(List<SysUser> users) {
        SysRole role = new SysRole();
        role.setId(8L);
        role.setRoleCode("LECTURER");
        when(roleMapper.selectOne(any())).thenReturn(role);
        List<SysUserRole> relations = users.stream().map(user -> {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(user.getId());
            relation.setRoleId(role.getId());
            return relation;
        }).toList();
        when(userRoleMapper.selectList(any())).thenReturn(relations);
        when(userMapper.selectList(any())).thenReturn(users);
    }

    private SysTeacherScheduleCalendarReq request(Long teacherId) {
        SysTeacherScheduleCalendarReq req = new SysTeacherScheduleCalendarReq();
        req.setTeacherId(teacherId);
        req.setMonth(YearMonth.of(2026, 7));
        return req;
    }

    private SysUser teacher(Long id, String realName, String username) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setRealName(realName);
        user.setUsername(username);
        user.setStatus(ActiveEnum.ACTIVE.name());
        return user;
    }

    private TeacherScheduleRow row(Long id, LocalDate date, Long classId,
                                   String className, String content) {
        TeacherScheduleRow row = new TeacherScheduleRow();
        row.setScheduleId(id);
        row.setScheduleDate(date);
        row.setClassId(classId);
        row.setClassName(className);
        row.setCourseDetailId(301L);
        row.setStageName("第一阶段");
        row.setCourseContent(content);
        return row;
    }

    private void initTableInfo(Class<?> entityType) {
        if (TableInfoHelper.getTableInfo(entityType) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), ""), entityType);
        }
    }
}
