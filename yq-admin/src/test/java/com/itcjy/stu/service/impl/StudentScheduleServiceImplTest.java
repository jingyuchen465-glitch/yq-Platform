package com.itcjy.stu.service.impl;

import com.itcjy.emp.pojo.res.academic.SysClassScheduleRes;
import com.itcjy.emp.service.academic.ISysClassScheduleService;
import com.itcjy.stu.pojo.DTO.StudentScheduleQueryDTO;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import com.itcjy.stu.pojo.VO.StudentScheduleVO;
import com.itcjy.stu.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StudentScheduleServiceImplTest {

    private LoginService loginService;
    private ISysClassScheduleService classScheduleService;
    private StudentScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        loginService = mock(LoginService.class);
        classScheduleService = mock(ISysClassScheduleService.class);
        service = new StudentScheduleServiceImpl(loginService, classScheduleService);
    }

    @Test
    @DisplayName("未分班学生返回未分班状态且不查询班级课表")
    void shouldReturnUnassignedWhenStudentHasNoClass() {
        StudentDetailsVO student = new StudentDetailsVO();
        student.setId(9L);
        when(loginService.getCurrentStudent()).thenReturn(student);

        StudentScheduleVO result = service.getCurrentStudentSchedule(new StudentScheduleQueryDTO());

        assertThat(result.classAssigned()).isFalse();
        assertThat(result.classId()).isNull();
        assertThat(result.courses()).isEmpty();
        verifyNoInteractions(classScheduleService);
    }

    @Test
    @DisplayName("已分班学生仅获得请求日期范围内的课程")
    void shouldReturnCoursesWithinRequestedDateRange() {
        StudentDetailsVO student = new StudentDetailsVO();
        student.setId(9L);
        student.setClassId(42L);
        when(loginService.getCurrentStudent()).thenReturn(student);
        when(classScheduleService.listSchedule(42L)).thenReturn(List.of(
                schedule(101L, LocalDate.of(2026, 7, 26), "范围外课程"),
                schedule(102L, LocalDate.of(2026, 7, 27), "Java 基础"),
                schedule(103L, LocalDate.of(2026, 8, 2), "集合框架"),
                schedule(104L, LocalDate.of(2026, 8, 3), "范围外课程")
        ));
        StudentScheduleQueryDTO query = new StudentScheduleQueryDTO();
        query.setStartDate(LocalDate.of(2026, 7, 27));
        query.setEndDate(LocalDate.of(2026, 8, 2));

        StudentScheduleVO result = service.getCurrentStudentSchedule(query);

        assertThat(result.classAssigned()).isTrue();
        assertThat(result.classId()).isEqualTo(42L);
        assertThat(result.courses()).extracting(course -> course.id())
                .containsExactly(102L, 103L);
        assertThat(result.courses()).extracting(course -> course.courseContent())
                .containsExactly("Java 基础", "集合框架");
    }

    private SysClassScheduleRes schedule(Long id, LocalDate scheduleDate, String content) {
        return new SysClassScheduleRes(
                id,
                scheduleDate,
                31L,
                "第一阶段",
                content,
                "CLASS",
                "上课",
                7L,
                "张老师"
        );
    }
}
