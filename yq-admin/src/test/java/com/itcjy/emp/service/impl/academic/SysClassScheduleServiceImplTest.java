package com.itcjy.emp.service.impl.academic;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.itcjy.emp.pojo.req.academic.SysClassScheduleTeacherAssignReq;
import com.itcjy.emp.pojo.req.academic.SysClassScheduleTemporaryCourseReq;
import com.itcjy.emp.pojo.req.academic.SysClassScheduleUpdateReq;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTeacherAssignRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTemporaryCourseRes;
import com.itcjy.emp.pojo.res.academic.SysClassScheduleTemporaryCourseOptionsRes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.TransactionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysClassScheduleServiceImplTest {

    @Mock
    private SysClassMapper sysClassMapper;
    @Mock
    private SysClassScheduleMapper sysClassScheduleMapper;
    @Mock
    private SysCourseDetailMapper sysCourseDetailMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private HolidayUtil holidayUtil;
    @Mock
    private TransactionTemplate transactionTemplate;

    private SysClassScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(SysClassSchedule.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                    SysClassSchedule.class);
        }
        service = spy(new SysClassScheduleServiceImpl(
                sysClassMapper,
                sysCourseDetailMapper,
                sysRoleMapper,
                sysUserMapper,
                sysUserRoleMapper,
                holidayUtil,
                transactionTemplate));
        ReflectionTestUtils.setField(service, "baseMapper", sysClassScheduleMapper);
    }

    @Test
    @DisplayName("课表尚未分配教师时正常返回空教师信息")
    void shouldListScheduleWhenTeacherIsUnassigned() {
        SysClass sysClass = new SysClass();
        sysClass.setId(14L);
        SysClassSchedule schedule = teachingSchedule(
                1L, 14L, null, 901L, LocalDate.of(2026, 7, 27));
        SysCourseDetail detail = courseDetail(901L, 7L, "阶段一");
        when(sysClassMapper.selectById(14L)).thenReturn(sysClass);
        when(sysClassScheduleMapper.selectList(any())).thenReturn(List.of(schedule));
        when(sysCourseDetailMapper.selectBatchIds(List.of(901L))).thenReturn(List.of(detail));

        List<SysClassScheduleRes> result = service.listSchedule(14L);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.stageName()).isEqualTo("阶段一");
            assertThat(item.teacherId()).isNull();
            assertThat(item.teacherName()).isNull();
        });
    }

    @Test
    @DisplayName("教师同一天已在其他班级授课时拒绝分配")
    void shouldRejectWhenTeacherHasClassOnSameDate() {
        SysClass sysClass = classEntity(14L, 7L);
        SysCourseDetail detail = courseDetail(901L, 7L, "阶段一");
        SysClassSchedule stageSchedule = teachingSchedule(
                1L, 14L, null, 901L, LocalDate.of(2026, 7, 28));
        SysUser teacher = activeTeacher(41L, "张老师", "lecturer01");
        SysClassSchedule conflict = teachingSchedule(99L, 20L, 41L, 901L, LocalDate.of(2026, 7, 28));
        SysClass conflictClass = new SysClass();
        conflictClass.setId(20L);
        conflictClass.setClassPeriod("Java-02期");

        when(sysClassMapper.selectByIdForUpdate(14L)).thenReturn(sysClass);
        when(sysCourseDetailMapper.selectList(any())).thenReturn(List.of(detail));
        when(sysClassScheduleMapper.selectList(any())).thenReturn(List.of(stageSchedule));
        when(sysUserMapper.selectByIdForUpdate(41L)).thenReturn(teacher);
        when(sysRoleMapper.selectOne(any())).thenReturn(lecturerRole());
        when(sysUserRoleMapper.selectCount(any())).thenReturn(1L);
        when(sysClassScheduleMapper.selectOne(any(), eq(false))).thenReturn(conflict);
        when(sysClassMapper.selectById(20L)).thenReturn(conflictClass);

        assertThatThrownBy(() -> service.assignTeacherByStage(
                new SysClassScheduleTeacherAssignReq(14L, "阶段一", 41L)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(16003);
                    assertThat(exception.getMessage()).contains("2026-07-28", "Java-02期");
                });
    }

    @Test
    @DisplayName("按阶段校验通过后一次性更新该阶段全部上课日")
    @SuppressWarnings("unchecked")
    void shouldAssignTeacherToWholeStage() {
        SysClass sysClass = new SysClass();
        sysClass.setId(14L);
        sysClass.setCourseId(7L);
        SysCourseDetail firstDetail = courseDetail(901L, 7L, "阶段一");
        SysCourseDetail secondDetail = courseDetail(902L, 7L, "阶段一");
        List<SysClassSchedule> stageSchedules = List.of(
                teachingSchedule(1L, 14L, null, 901L, LocalDate.of(2026, 7, 27)),
                teachingSchedule(2L, 14L, null, 902L, LocalDate.of(2026, 7, 28)));
        SysUser teacher = new SysUser();
        teacher.setId(41L);
        teacher.setUsername("lecturer01");
        teacher.setRealName("张老师");
        teacher.setStatus(ActiveEnum.ACTIVE.name());
        SysRole lecturerRole = new SysRole();
        lecturerRole.setId(2L);
        lecturerRole.setRoleCode("LECTURER");

        when(sysClassMapper.selectByIdForUpdate(14L)).thenReturn(sysClass);
        when(sysCourseDetailMapper.selectList(any())).thenReturn(List.of(firstDetail, secondDetail));
        when(sysClassScheduleMapper.selectList(any())).thenReturn(stageSchedules);
        when(sysUserMapper.selectByIdForUpdate(41L)).thenReturn(teacher);
        when(sysRoleMapper.selectOne(any())).thenReturn(lecturerRole);
        when(sysUserRoleMapper.selectCount(any())).thenReturn(1L);
        when(sysClassScheduleMapper.selectOne(any(), eq(false))).thenReturn(null);
        when(sysClassScheduleMapper.update(isNull(), any(Wrapper.class))).thenReturn(2);

        SysClassScheduleTeacherAssignRes result = service.assignTeacherByStage(
                new SysClassScheduleTeacherAssignReq(14L, "阶段一", 41L));

        assertThat(result.teacherId()).isEqualTo(41L);
        assertThat(result.teacherName()).isEqualTo("张老师（lecturer01）");
        assertThat(result.assignedClassDays()).isEqualTo(2);
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 7, 27));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2026, 7, 28));
        InOrder lockOrder = inOrder(sysClassMapper, sysUserMapper);
        lockOrder.verify(sysClassMapper).selectByIdForUpdate(14L);
        lockOrder.verify(sysUserMapper).selectByIdForUpdate(41L);
        verify(sysClassScheduleMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    @DisplayName("临时加课选项按阶段去重并统计课程数量")
    void shouldListDistinctStageOptions() {
        SysClass sysClass = classEntity(14L, 7L);
        SysCourseDetail first = courseDetail(901L, 7L, "阶段一");
        first.setDayNumber(1);
        SysCourseDetail second = courseDetail(902L, 7L, "阶段一");
        second.setDayNumber(2);
        SysCourseDetail third = courseDetail(903L, 7L, "阶段二");
        third.setDayNumber(3);
        SysRole role = lecturerRole();
        SysUser teacher = activeTeacher(41L, "张老师", "lecturer01");
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(41L);
        userRole.setRoleId(role.getId());

        when(sysClassMapper.selectById(14L)).thenReturn(sysClass);
        when(sysCourseDetailMapper.selectList(any())).thenReturn(List.of(first, second, third));
        when(sysRoleMapper.selectOne(any())).thenReturn(role);
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(userRole));
        when(sysUserMapper.selectList(any())).thenReturn(List.of(teacher));

        SysClassScheduleTemporaryCourseOptionsRes result = service.listTemporaryCourseOptions(14L);

        assertThat(result.stages()).satisfiesExactly(
                stage -> {
                    assertThat(stage.stageName()).isEqualTo("阶段一");
                    assertThat(stage.courseCount()).isEqualTo(2);
                    assertThat(stage.label()).isEqualTo("阶段一（2节课程）");
                },
                stage -> {
                    assertThat(stage.stageName()).isEqualTo("阶段二");
                    assertThat(stage.courseCount()).isEqualTo(1);
                });
        assertThat(result.teachers()).singleElement();
    }

    @Test
    @DisplayName("单格修改阶段和教师时校验冲突并更新阶段锚点")
    void shouldUpdateSingleScheduleStageAndTeacher() {
        SysClass sysClass = classEntity(14L, 7L);
        SysClassSchedule schedule = teachingSchedule(
                1L, 14L, 50L, 901L, LocalDate.of(2026, 7, 27));
        schedule.setCourseContent("变量与数据类型");
        SysCourseDetail currentDetail = courseDetail(901L, 7L, "阶段一");
        SysCourseDetail targetStageAnchor = courseDetail(910L, 7L, "阶段二");
        SysUser teacher = activeTeacher(41L, "张老师", "lecturer01");

        when(sysClassScheduleMapper.selectById(1L)).thenReturn(schedule);
        when(sysClassMapper.selectByIdForUpdate(14L)).thenReturn(sysClass);
        when(sysUserMapper.selectByIdForUpdate(41L)).thenReturn(teacher);
        when(sysClassScheduleMapper.selectByIdForUpdate(1L)).thenReturn(schedule);
        when(sysRoleMapper.selectOne(any())).thenReturn(lecturerRole());
        when(sysUserRoleMapper.selectCount(any())).thenReturn(1L);
        when(sysCourseDetailMapper.selectById(901L)).thenReturn(currentDetail);
        when(sysCourseDetailMapper.selectList(any())).thenReturn(List.of(targetStageAnchor));
        when(sysClassScheduleMapper.selectOne(any(), eq(false))).thenReturn(null);
        when(sysClassScheduleMapper.updateById(any(SysClassSchedule.class))).thenReturn(1);

        SysClassScheduleRes result = service.updateSchedule(
                1L,
                new SysClassScheduleUpdateReq("阶段二", "项目实战与集中答疑", 41L));

        assertThat(result.stageName()).isEqualTo("阶段二");
        assertThat(result.teacherId()).isEqualTo(41L);
        assertThat(result.courseContent()).isEqualTo("项目实战与集中答疑");
        verify(sysClassScheduleMapper).selectOne(any(), eq(false));
        verify(sysClassScheduleMapper).updateById(schedule);
    }

    @Test
    @DisplayName("非节假日删除课程时后续课程和教师依次前移")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldShiftLaterCoursesForwardWhenDeletingNonHolidayClass() {
        SysClass sysClass = classEntity(14L, 7L);
        SysClassSchedule target = teachingSchedule(
                1L, 14L, 41L, 901L, LocalDate.of(2026, 7, 27));
        target.setCourseContent("课程一");
        SysClassSchedule next = teachingSchedule(
                2L, 14L, 50L, 902L, LocalDate.of(2026, 7, 28));
        next.setCourseContent("课程二");
        List<SysClassSchedule> schedules = List.of(target, next);
        when(sysClassScheduleMapper.selectById(1L)).thenReturn(target);
        when(holidayUtil.getHolidayInfo(LocalDate.of(2026, 7, 27))).thenReturn(null);
        when(sysClassScheduleMapper.selectList(any())).thenReturn(schedules);
        when(sysClassMapper.selectByIdForUpdate(14L)).thenReturn(sysClass);
        when(sysUserMapper.selectByIdForUpdate(50L)).thenReturn(activeTeacher(50L, "李老师", "lecturer02"));
        when(sysClassScheduleMapper.selectByIdForUpdate(1L)).thenReturn(target);
        when(sysClassScheduleMapper.selectByIdForUpdate(2L)).thenReturn(next);
        when(sysClassScheduleMapper.selectOne(any(), eq(false))).thenReturn(null);
        stubTransactionWithoutResult();
        doReturn(true).when(service).updateBatchById(anyList());
        doReturn(true).when(service).removeByIds(anyList());

        service.deleteSchedule(1L);

        ArgumentCaptor<List<SysClassSchedule>> updateCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(service).updateBatchById(updateCaptor.capture());
        assertThat(updateCaptor.getValue()).singleElement().satisfies(schedule -> {
            assertThat(schedule.getId()).isEqualTo(1L);
            assertThat(schedule.getCourseDetailId()).isEqualTo(902L);
            assertThat(schedule.getCourseContent()).isEqualTo("课程二");
            assertThat(schedule.getTeacherId()).isEqualTo(50L);
        });
        ArgumentCaptor<List<Long>> deleteCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(service).removeByIds(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue()).containsExactly(2L);
        verify(sysClassScheduleMapper).selectOne(any(), eq(false));
    }

    @Test
    @DisplayName("删除最后一节非节假日课程时清空整张课表")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldClearScheduleWhenDeletingLastNonHolidayClass() {
        SysClass sysClass = classEntity(14L, 7L);
        SysClassSchedule target = teachingSchedule(
                1L, 14L, 41L, 901L, LocalDate.of(2026, 7, 27));
        SysClassSchedule trailingRest = nonClassSchedule(
                2L, 14L, LocalDate.of(2026, 7, 28), ClassScheduleConstants.DayType.REST, "休息");
        List<SysClassSchedule> schedules = List.of(target, trailingRest);
        when(sysClassScheduleMapper.selectById(1L)).thenReturn(target);
        when(holidayUtil.getHolidayInfo(LocalDate.of(2026, 7, 27))).thenReturn(null);
        when(sysClassScheduleMapper.selectList(any())).thenReturn(schedules);
        when(sysClassMapper.selectByIdForUpdate(14L)).thenReturn(sysClass);
        when(sysClassScheduleMapper.selectByIdForUpdate(1L)).thenReturn(target);
        when(sysClassScheduleMapper.selectByIdForUpdate(2L)).thenReturn(trailingRest);
        stubTransactionWithoutResult();
        doReturn(true).when(service).removeByIds(anyList());

        service.deleteSchedule(1L);

        ArgumentCaptor<List<Long>> deleteCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(service).removeByIds(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("删除节假日临时课程时恢复节假日日程")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldRestoreHolidayWhenDeletingHolidayClass() {
        SysClass sysClass = classEntity(14L, 7L);
        SysClassSchedule target = teachingSchedule(
                1L, 14L, 41L, 901L, LocalDate.of(2026, 10, 1));
        target.setCourseContent("国庆临时课程");
        HolidayInfo holidayInfo = new HolidayInfo();
        holidayInfo.setHoliday(true);
        holidayInfo.setName("国庆节");
        when(sysClassScheduleMapper.selectById(1L)).thenReturn(target);
        when(holidayUtil.getHolidayInfo(LocalDate.of(2026, 10, 1))).thenReturn(holidayInfo);
        when(sysClassScheduleMapper.selectList(any())).thenReturn(List.of(target));
        when(sysClassMapper.selectByIdForUpdate(14L)).thenReturn(sysClass);
        when(sysClassScheduleMapper.selectByIdForUpdate(1L)).thenReturn(target);
        stubTransactionWithoutResult();
        doReturn(true).when(service).updateBatchById(anyList());

        service.deleteSchedule(1L);

        ArgumentCaptor<List<SysClassSchedule>> updateCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(service).updateBatchById(updateCaptor.capture());
        assertThat(updateCaptor.getValue()).singleElement().satisfies(schedule -> {
            assertThat(schedule.getClassType()).isEqualTo(ClassScheduleConstants.DayType.HOLIDAY);
            assertThat(schedule.getCourseContent()).isEqualTo("国庆节");
            assertThat(schedule.getCourseDetailId()).isNull();
            assertThat(schedule.getTeacherId()).isNull();
        });
    }

    @SuppressWarnings("unchecked")
    private void stubTransactionWithoutResult() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any(Consumer.class));
    }

    @Test
    @DisplayName("非上课日临时加课时直接替换当天日程")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldAddTemporaryCourseDirectlyOnNonClassDay() {
        SysClass sysClass = classEntity(14L, 7L);
        SysCourseDetail temporaryDetail = courseDetail(903L, 7L, "临时专题");
        temporaryDetail.setClassContent("项目答疑");
        SysUser teacher = activeTeacher(41L, "张老师", "lecturer01");
        SysRole lecturerRole = lecturerRole();
        SysClassSchedule classSchedule = teachingSchedule(
                1L, 14L, 50L, 901L, LocalDate.of(2026, 7, 29));
        SysClassSchedule selfStudySchedule = nonClassSchedule(
                2L, 14L, LocalDate.of(2026, 7, 30), ClassScheduleConstants.DayType.SELF_STUDY, "自习");
        List<SysClassSchedule> schedules = List.of(classSchedule, selfStudySchedule);

        stubTemporaryCourseDependencies(sysClass, temporaryDetail, teacher, lecturerRole, schedules);
        doReturn(true).when(service).updateBatchById(anyList());

        SysClassScheduleTemporaryCourseRes result = service.addTemporaryCourse(
                new SysClassScheduleTemporaryCourseReq(
                        14L, "临时专题", 41L, LocalDate.of(2026, 7, 30)));

        assertThat(result.shifted()).isFalse();
        assertThat(result.shiftedClassCount()).isZero();
        ArgumentCaptor<List<SysClassSchedule>> updateCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(service).updateBatchById(updateCaptor.capture());
        assertThat(updateCaptor.getValue()).singleElement().satisfies(schedule -> {
            assertThat(schedule.getScheduleDate()).isEqualTo(LocalDate.of(2026, 7, 30));
            assertThat(schedule.getClassType()).isEqualTo(ClassScheduleConstants.DayType.CLASS);
            assertThat(schedule.getCourseDetailId()).isEqualTo(903L);
            assertThat(schedule.getTeacherId()).isEqualTo(41L);
        });
    }

    @Test
    @DisplayName("上课日临时加课时原课程及教师依次顺延")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldShiftCoursesAndTeachersWhenTargetDateAlreadyHasClass() {
        SysClass sysClass = classEntity(14L, 7L);
        SysCourseDetail temporaryDetail = courseDetail(903L, 7L, "临时专题");
        temporaryDetail.setClassContent("项目答疑");
        SysUser temporaryTeacher = activeTeacher(41L, "张老师", "lecturer01");
        SysUser originalTeacher = activeTeacher(50L, "李老师", "lecturer02");
        SysRole lecturerRole = lecturerRole();
        List<SysClassSchedule> schedules = List.of(
                teachingSchedule(1L, 14L, 50L, 901L, LocalDate.of(2026, 7, 27)),
                teachingSchedule(2L, 14L, 50L, 902L, LocalDate.of(2026, 7, 28)));

        stubTemporaryCourseDependencies(sysClass, temporaryDetail, temporaryTeacher, lecturerRole, schedules);
        when(sysUserMapper.selectByIdForUpdate(50L)).thenReturn(originalTeacher);
        when(holidayUtil.getHolidayInfo(LocalDate.of(2026, 7, 29))).thenReturn(null);
        doReturn(true).when(service).updateBatchById(anyList());
        doReturn(true).when(service).saveBatch(anyList());

        SysClassScheduleTemporaryCourseRes result = service.addTemporaryCourse(
                new SysClassScheduleTemporaryCourseReq(
                        14L, "临时专题", 41L, LocalDate.of(2026, 7, 27)));

        assertThat(result.shifted()).isTrue();
        assertThat(result.shiftedClassCount()).isEqualTo(2);
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2026, 7, 29));

        ArgumentCaptor<List<SysClassSchedule>> updateCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(service).updateBatchById(updateCaptor.capture());
        assertThat(updateCaptor.getValue()).satisfiesExactly(
                schedule -> {
                    assertThat(schedule.getScheduleDate()).isEqualTo(LocalDate.of(2026, 7, 27));
                    assertThat(schedule.getCourseDetailId()).isEqualTo(903L);
                    assertThat(schedule.getTeacherId()).isEqualTo(41L);
                },
                schedule -> {
                    assertThat(schedule.getScheduleDate()).isEqualTo(LocalDate.of(2026, 7, 28));
                    assertThat(schedule.getCourseDetailId()).isEqualTo(901L);
                    assertThat(schedule.getTeacherId()).isEqualTo(50L);
                });

        ArgumentCaptor<List<SysClassSchedule>> insertCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(service).saveBatch(insertCaptor.capture());
        assertThat(insertCaptor.getValue()).singleElement().satisfies(schedule -> {
            assertThat(schedule.getScheduleDate()).isEqualTo(LocalDate.of(2026, 7, 29));
            assertThat(schedule.getCourseDetailId()).isEqualTo(902L);
            assertThat(schedule.getTeacherId()).isEqualTo(50L);
        });
        verify(sysClassScheduleMapper, times(2)).selectOne(any(), eq(false));
    }

    @SuppressWarnings("unchecked")
    private void stubTemporaryCourseDependencies(
            SysClass sysClass,
            SysCourseDetail courseDetail,
            SysUser teacher,
            SysRole lecturerRole,
            List<SysClassSchedule> schedules) {
        when(sysClassMapper.selectById(sysClass.getId())).thenReturn(sysClass);
        when(sysClassMapper.selectByIdForUpdate(sysClass.getId())).thenReturn(sysClass);
        when(sysCourseDetailMapper.selectList(any())).thenReturn(List.of(courseDetail));
        when(sysUserMapper.selectById(teacher.getId())).thenReturn(teacher);
        when(sysUserMapper.selectByIdForUpdate(teacher.getId())).thenReturn(teacher);
        when(sysRoleMapper.selectOne(any())).thenReturn(lecturerRole);
        when(sysUserRoleMapper.selectCount(any())).thenReturn(1L);
        when(sysClassScheduleMapper.selectList(any())).thenReturn(schedules);
        when(sysClassScheduleMapper.selectOne(any(), eq(false))).thenReturn(null);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(org.springframework.transaction.TransactionStatus.class));
        });
    }

    private SysClass classEntity(Long id, Long courseId) {
        SysClass sysClass = new SysClass();
        sysClass.setId(id);
        sysClass.setCourseId(courseId);
        return sysClass;
    }

    private SysRole lecturerRole() {
        SysRole role = new SysRole();
        role.setId(2L);
        role.setRoleCode("LECTURER");
        return role;
    }

    private SysUser activeTeacher(Long id, String realName, String username) {
        SysUser teacher = new SysUser();
        teacher.setId(id);
        teacher.setRealName(realName);
        teacher.setUsername(username);
        teacher.setStatus(ActiveEnum.ACTIVE.name());
        return teacher;
    }

    private SysClassSchedule nonClassSchedule(
            Long id,
            Long classId,
            LocalDate scheduleDate,
            String classType,
            String courseContent) {
        SysClassSchedule schedule = new SysClassSchedule();
        schedule.setId(id);
        schedule.setClassId(classId);
        schedule.setScheduleDate(scheduleDate);
        schedule.setClassType(classType);
        schedule.setCourseContent(courseContent);
        return schedule;
    }

    private SysCourseDetail courseDetail(Long id, Long courseId, String stageName) {
        SysCourseDetail detail = new SysCourseDetail();
        detail.setId(id);
        detail.setCourseId(courseId);
        detail.setStageName(stageName);
        return detail;
    }

    private SysClassSchedule teachingSchedule(
            Long id,
            Long classId,
            Long teacherId,
            Long courseDetailId,
            LocalDate scheduleDate) {
        SysClassSchedule schedule = new SysClassSchedule();
        schedule.setId(id);
        schedule.setClassId(classId);
        schedule.setTeacherId(teacherId);
        schedule.setCourseDetailId(courseDetailId);
        schedule.setScheduleDate(scheduleDate);
        schedule.setClassType(ClassScheduleConstants.DayType.CLASS);
        return schedule;
    }
}
