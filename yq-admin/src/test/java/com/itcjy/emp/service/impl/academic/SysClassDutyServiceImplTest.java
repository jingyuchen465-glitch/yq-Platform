package com.itcjy.emp.service.impl.academic;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.itcjy.emp.pojo.res.system.ClassScheduleRuleRes;
import com.itcjy.emp.service.system.ISysConfigService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysClassDutyServiceImplTest {

    @Mock
    private SysClassDutyMapper dutyMapper;
    @Mock
    private SysClassScheduleMapper classScheduleMapper;
    @Mock
    private SysCampusMapper campusMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private ISysConfigService sysConfigService;
    @Mock
    private HolidayUtil holidayUtil;

    private SysClassDutyServiceImpl service;

    @BeforeEach
    void setUp() {
        initTableInfo(SysRole.class);
        initTableInfo(SysUserRole.class);
        initTableInfo(SysUser.class);
        lenient().when(sysConfigService.getClassScheduleRule()).thenReturn(new ClassScheduleRuleRes(
                List.of(1, 2, 3, 5, 6), List.of(4), List.of(7), true));
        lenient().when(holidayUtil.getHolidayInfo(any(LocalDate.class))).thenReturn(null);
        service = new SysClassDutyServiceImpl(
                dutyMapper, classScheduleMapper, campusMapper,
                roleMapper, userRoleMapper, userMapper, sysConfigService, holidayUtil);
    }

    @Test
    @DisplayName("日视图只展示教学周期内班级，并单列未生成课表班级")
    void shouldBuildDailyRosterFromTeachingPeriods() {
        LocalDate dutyDate = LocalDate.of(2026, 7, 24);
        when(campusMapper.selectById(2L)).thenReturn(campus(2L, "杭州校区"));
        when(classScheduleMapper.selectClassTeachingPeriods(2L)).thenReturn(List.of(
                period(11L, "Java十一期", 2L,
                        LocalDate.of(2026, 3, 9), LocalDate.of(2026, 8, 14)),
                period(12L, "Java十二期", 2L,
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 20)),
                period(13L, "Java十三期", 2L, null, null)));

        SysClassDuty campusDuty = duty(100L, null, 31L, "EVENING_STUDY_CAMPUS");
        SysClassDuty classDuty = duty(101L, 11L, 32L, "EVENING_STUDY_CLASS");
        when(dutyMapper.selectDaily(2L, dutyDate)).thenReturn(List.of(campusDuty, classDuty));
        when(userMapper.selectBatchIds(List.of(31L, 32L)))
                .thenReturn(List.of(teacher(31L, "陈老师"), teacher(32L, "张老师")));
        when(classScheduleMapper.selectTeachingTeacherIds(dutyDate, dutyDate)).thenReturn(List.of(32L));

        SysClassDutyDailyRes result = service.getDailyDuties(dailyReq(2L, dutyDate));

        assertThat(result.campusName()).isEqualTo("杭州校区");
        assertThat(result.dutyMode()).isEqualTo("EVENING_STUDY");
        assertThat(result.campusDuty().teacherName()).contains("陈老师");
        assertThat(result.eveningClassDuties()).singleElement()
                .satisfies(row -> {
                    assertThat(row.className()).isEqualTo("Java十一期");
                    assertThat(row.assignment().teacherName()).contains("张老师");
                });
        assertThat(result.selfStudyClassDuties()).isEmpty();
        assertThat(result.unscheduledClasses()).singleElement()
                .satisfies(row -> assertThat(row.className()).isEqualTo("Java十三期"));
        assertThat(result.busyTeacherIds()).containsExactly(32L);
    }

    @Test
    @DisplayName("自习日只返回自习日班级值班")
    void shouldOnlyReturnSelfStudyDutiesOnConfiguredSelfStudyDay() {
        LocalDate dutyDate = LocalDate.of(2026, 7, 23);
        when(campusMapper.selectById(2L)).thenReturn(campus(2L, "杭州校区"));
        when(classScheduleMapper.selectClassTeachingPeriods(2L)).thenReturn(List.of(
                period(11L, "Java十一期", 2L,
                        LocalDate.of(2026, 3, 9), LocalDate.of(2026, 8, 14))));
        when(dutyMapper.selectDaily(2L, dutyDate)).thenReturn(List.of());
        when(classScheduleMapper.selectTeachingTeacherIds(dutyDate, dutyDate)).thenReturn(List.of());

        SysClassDutyDailyRes result = service.getDailyDuties(dailyReq(2L, dutyDate));

        assertThat(result.dutyMode()).isEqualTo("SELF_STUDY");
        assertThat(result.campusDuty()).isNull();
        assertThat(result.eveningClassDuties()).isEmpty();
        assertThat(result.selfStudyClassDuties()).singleElement()
                .satisfies(row -> assertThat(row.className()).isEqualTo("Java十一期"));
    }

    @Test
    @DisplayName("RESTDAYS 配置日不返回任何值班")
    void shouldReturnNoDutyOnConfiguredRestDay() {
        LocalDate dutyDate = LocalDate.of(2026, 7, 26);
        when(campusMapper.selectById(2L)).thenReturn(campus(2L, "杭州校区"));

        SysClassDutyDailyRes result = service.getDailyDuties(dailyReq(2L, dutyDate));

        assertThat(result.dutyMode()).isEqualTo("NO_DUTY");
        assertThat(result.campusDuty()).isNull();
        assertThat(result.eveningClassDuties()).isEmpty();
        assertThat(result.selfStudyClassDuties()).isEmpty();
        assertThat(result.busyTeacherIds()).isEmpty();
        verify(classScheduleMapper, never()).selectClassTeachingPeriods(any());
    }

    @Test
    @DisplayName("法定节假日优先于星期配置且不返回任何值班")
    void shouldReturnNoDutyOnPublicHoliday() {
        LocalDate dutyDate = LocalDate.of(2026, 7, 24);
        when(campusMapper.selectById(2L)).thenReturn(campus(2L, "杭州校区"));
        when(holidayUtil.getHolidayInfo(dutyDate)).thenReturn(new HolidayInfo(true, "测试节日"));

        SysClassDutyDailyRes result = service.getDailyDuties(dailyReq(2L, dutyDate));

        assertThat(result.dutyMode()).isEqualTo("NO_DUTY");
        assertThat(result.eveningClassDuties()).isEmpty();
        assertThat(result.selfStudyClassDuties()).isEmpty();
        verify(classScheduleMapper, never()).selectClassTeachingPeriods(any());
    }

    @Test
    @DisplayName("自习日拒绝保存晚自习值班")
    void shouldRejectEveningDutyOnSelfStudyDay() {
        LocalDate dutyDate = LocalDate.of(2026, 7, 23);
        when(campusMapper.selectById(2L)).thenReturn(campus(2L, "杭州校区"));
        SysClassDutySaveReq request = new SysClassDutySaveReq(
                2L, 11L, 31L, dutyDate, "EVENING_STUDY_CLASS", null);

        assertThatThrownBy(() -> service.saveDuty(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自习日不能安排晚自习值班");
        verify(dutyMapper, never()).insert(any(SysClassDuty.class));
    }

    @Test
    @DisplayName("非自习日拒绝保存自习日值班")
    void shouldRejectSelfStudyDutyOnEveningStudyDay() {
        LocalDate dutyDate = LocalDate.of(2026, 7, 24);
        when(campusMapper.selectById(2L)).thenReturn(campus(2L, "杭州校区"));
        SysClassDutySaveReq request = new SysClassDutySaveReq(
                2L, 11L, 31L, dutyDate, "SELF_STUDY_CLASS", null);

        assertThatThrownBy(() -> service.saveDuty(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非自习日不能安排自习日值班");
        verify(dutyMapper, never()).insert(any(SysClassDuty.class));
    }

    @Test
    @DisplayName("节假日或休息日拒绝保存任何值班")
    void shouldRejectDutyOnRestDay() {
        LocalDate dutyDate = LocalDate.of(2026, 7, 26);
        when(campusMapper.selectById(2L)).thenReturn(campus(2L, "杭州校区"));
        SysClassDutySaveReq request = new SysClassDutySaveReq(
                2L, 11L, 31L, dutyDate, "EVENING_STUDY_CLASS", null);

        assertThatThrownBy(() -> service.saveDuty(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("节假日或休息日不能安排值班");
        verify(dutyMapper, never()).insert(any(SysClassDuty.class));
    }

    @Test
    @DisplayName("保存班级值班时由后端写入固定类型和时间")
    void shouldSaveDutyWithServerControlledTime() {
        LocalDate dutyDate = LocalDate.of(2026, 7, 24);
        when(campusMapper.selectById(2L)).thenReturn(campus(2L, "杭州校区"));
        when(classScheduleMapper.selectClassTeachingPeriod(11L)).thenReturn(
                period(11L, "Java十一期", 2L,
                        LocalDate.of(2026, 3, 9), LocalDate.of(2026, 8, 14)));
        mockActiveLecturer(31L);
        when(dutyMapper.selectScopeForUpdate(
                2L, 11L, dutyDate, "EVENING_STUDY_CLASS")).thenReturn(null);
        when(dutyMapper.selectTeacherDuties(31L, dutyDate)).thenReturn(List.of());

        service.saveDuty(new SysClassDutySaveReq(
                2L, 11L, 31L, dutyDate, "EVENING_STUDY_CLASS", " 晚间巡班 "));

        ArgumentCaptor<SysClassDuty> captor = ArgumentCaptor.forClass(SysClassDuty.class);
        verify(dutyMapper).insert(captor.capture());
        SysClassDuty saved = captor.getValue();
        assertThat(saved.getDutyType()).isEqualTo("EVENING_STUDY_CLASS");
        assertThat(saved.getStartTime()).isEqualTo("19:00");
        assertThat(saved.getEndTime()).isEqualTo("21:00");
        assertThat(saved.getRemark()).isEqualTo("晚间巡班");
    }

    @Test
    @DisplayName("同一老师不能承担时间重叠的值班")
    void shouldRejectOverlappingTeacherDuty() {
        LocalDate dutyDate = LocalDate.of(2026, 7, 24);
        when(campusMapper.selectById(2L)).thenReturn(campus(2L, "杭州校区"));
        when(classScheduleMapper.selectClassTeachingPeriod(11L)).thenReturn(
                period(11L, "Java十一期", 2L,
                        LocalDate.of(2026, 3, 9), LocalDate.of(2026, 8, 14)));
        mockActiveLecturer(31L);
        when(dutyMapper.selectScopeForUpdate(
                2L, 11L, dutyDate, "EVENING_STUDY_CLASS")).thenReturn(null);
        when(dutyMapper.selectTeacherDuties(31L, dutyDate)).thenReturn(List.of(
                duty(90L, 15L, 31L, "EVENING_STUDY_CLASS")));

        SysClassDutySaveReq request = new SysClassDutySaveReq(
                2L, 11L, 31L, dutyDate, "EVENING_STUDY_CLASS", null);

        assertThatThrownBy(() -> service.saveDuty(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已有其他值班");
        verify(dutyMapper, never()).insert(any(SysClassDuty.class));
    }

    private void mockActiveLecturer(Long teacherId) {
        SysRole role = new SysRole();
        role.setId(8L);
        role.setRoleCode("LECTURER");
        when(roleMapper.selectOne(any())).thenReturn(role);
        when(userMapper.selectById(teacherId)).thenReturn(teacher(teacherId, "陈老师"));
        when(userRoleMapper.selectCount(any())).thenReturn(1L);
    }

    private SysClassDutyDailyReq dailyReq(Long campusId, LocalDate dutyDate) {
        SysClassDutyDailyReq req = new SysClassDutyDailyReq();
        req.setCampusId(campusId);
        req.setDutyDate(dutyDate);
        return req;
    }

    private SysCampus campus(Long id, String name) {
        SysCampus campus = new SysCampus();
        campus.setId(id);
        campus.setCampusLocation(name);
        return campus;
    }

    private ClassTeachingPeriodRow period(Long id, String name, Long campusId,
                                          LocalDate startDate, LocalDate endDate) {
        ClassTeachingPeriodRow row = new ClassTeachingPeriodRow();
        row.setClassId(id);
        row.setClassName(name);
        row.setCampusId(campusId);
        row.setTeachingStartDate(startDate);
        row.setTeachingEndDate(endDate);
        return row;
    }

    private SysClassDuty duty(Long id, Long classId, Long teacherId, String dutyType) {
        SysClassDuty duty = new SysClassDuty();
        duty.setId(id);
        duty.setCampusId(2L);
        duty.setClassId(classId);
        duty.setTeacherId(teacherId);
        duty.setDutyDate(LocalDate.of(2026, 7, 24));
        duty.setDutyType(dutyType);
        return duty;
    }

    private SysUser teacher(Long id, String name) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setRealName(name);
        user.setUsername("teacher" + id);
        user.setStatus(ActiveEnum.ACTIVE.name());
        return user;
    }

    private void initTableInfo(Class<?> entityType) {
        if (TableInfoHelper.getTableInfo(entityType) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), ""), entityType);
        }
    }
}
