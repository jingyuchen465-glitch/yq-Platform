package com.itcjy.emp.service.impl.stu;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcjy.common.constants.TokenConstants;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysClass;
import com.itcjy.emp.pojo.req.stu.StudentPageReq;
import com.itcjy.emp.pojo.req.stu.StudentUpdateReq;
import com.itcjy.emp.pojo.res.stu.StudentClassOptionRes;
import com.itcjy.emp.pojo.res.stu.StudentRes;
import com.itcjy.emp.service.academic.ISysClassService;
import com.itcjy.stu.mapper.LoginMapper;
import com.itcjy.stu.pojo.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentServiceImplTest {

    private LoginMapper loginMapper;
    private ISysClassService sysClassService;
    private RedisTemplate<String, Object> redisTemplate;
    private StudentServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "student-test"),
                Student.class
        );
        loginMapper = mock(LoginMapper.class);
        sysClassService = mock(ISysClassService.class);
        redisTemplate = mock(RedisTemplate.class);
        service = new StudentServiceImpl(sysClassService, redisTemplate);
        ReflectionTestUtils.setField(service, "baseMapper", loginMapper);
    }

    @Test
    @DisplayName("应按关键字、班级和状态分页并补充班级名称")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shouldPageStudentsWithFiltersAndClassName() {
        StudentPageReq req = new StudentPageReq();
        req.setCurrent(2L);
        req.setSize(20L);
        req.setKeyword(" 张 ");
        req.setClassId(9L);
        req.setStatus(Student.AT_SCHOOL);

        Student student = student(7L, Student.AT_SCHOOL);
        student.setClassId(9L);
        doAnswer(invocation -> {
            Page<Student> page = invocation.getArgument(0);
            page.setRecords(List.of(student));
            page.setTotal(21L);
            return page;
        }).when(loginMapper).selectPage(any(Page.class), any(Wrapper.class));
        SysClass sysClass = sysClass(9L, "Java第18期");
        when(sysClassService.listByIds(anyCollection())).thenReturn(List.of(sysClass));

        PageResult<StudentRes> result = service.pageStudents(req);

        assertThat(result.getTotal()).isEqualTo(21L);
        assertThat(result.getRecords()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(7L);
            assertThat(item.className()).isEqualTo("Java第18期");
            assertThat(item.statusDesc()).isEqualTo("在校学习");
        });
        ArgumentCaptor<Wrapper<Student>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(loginMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment())
                .contains("name", "phone", "email", "class_id", "status");
        LambdaQueryWrapper<Student> queryWrapper = (LambdaQueryWrapper<Student>) wrapperCaptor.getValue();
        assertThat(queryWrapper.getParamNameValuePairs().values())
                .contains(9L, Student.AT_SCHOOL)
                .anySatisfy(value -> assertThat(value).isEqualTo("%张%"));
    }

    @Test
    @DisplayName("应返回不包含密码的学员详情")
    void shouldReturnStudentDetailWithoutPassword() {
        Student student = student(7L, Student.TEMPORARY);
        student.setPassword("bcrypt-secret");
        student.setClassId(9L);
        when(loginMapper.selectById(7L)).thenReturn(student);
        when(sysClassService.getById(9L)).thenReturn(sysClass(9L, "Java第18期"));

        StudentRes result = service.getStudentDetail(7L);

        assertThat(result.className()).isEqualTo("Java第18期");
        assertThat(Arrays.stream(StudentRes.class.getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("password");
    }

    @Test
    @DisplayName("学员不存在时应返回明确业务异常")
    void shouldRejectMissingStudent() {
        when(loginMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.getStudentDetail(99L))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getCode()).isEqualTo(BusinessException.STUDENT_NOT_EXIST.getCode());
                    assertThat(error.getMessage()).isEqualTo("学员不存在");
                });
    }

    @Test
    @DisplayName("修改学员资料时应规范化字段并终止旧会话")
    void shouldUpdateStudentAndEvictSession() {
        Student student = student(7L, Student.TEMPORARY);
        when(loginMapper.selectById(7L)).thenReturn(student);
        when(loginMapper.selectCount(any())).thenReturn(0L);
        when(sysClassService.getById(9L)).thenReturn(sysClass(9L, "Java第18期"));
        when(loginMapper.updateById(student)).thenReturn(1);

        service.updateStudent(7L, new StudentUpdateReq(
                "  张三  ",
                " 13800138000 ",
                "   ",
                9L,
                Student.AT_SCHOOL
        ));

        assertThat(student.getName()).isEqualTo("张三");
        assertThat(student.getPhone()).isEqualTo("13800138000");
        assertThat(student.getEmail()).isNull();
        assertThat(student.getClassId()).isEqualTo(9L);
        assertThat(student.getStatus()).isEqualTo(Student.AT_SCHOOL);
        verify(redisTemplate).delete(TokenConstants.STUDENT_JWT_KEY_PREFIX + 7L);
    }

    @Test
    @DisplayName("手机号被其他学员使用时应拒绝修改")
    void shouldRejectDuplicatePhone() {
        Student student = student(7L, Student.TEMPORARY);
        when(loginMapper.selectById(7L)).thenReturn(student);
        when(loginMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.updateStudent(7L, updateReq(null, Student.TEMPORARY)))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getCode()).isEqualTo(BusinessException.STUDENT_EXIST.getCode()));

        verify(loginMapper, never()).updateById(any(Student.class));
        verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    @DisplayName("所选班级不存在时应拒绝修改")
    void shouldRejectMissingClass() {
        Student student = student(7L, Student.TEMPORARY);
        when(loginMapper.selectById(7L)).thenReturn(student);
        when(loginMapper.selectCount(any())).thenReturn(0L);
        when(sysClassService.getById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateStudent(7L, updateReq(9L, Student.AT_SCHOOL)))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getCode()).isEqualTo(BusinessException.CLAZZ_NOT_EXIST.getCode()));

        verify(loginMapper, never()).updateById(any(Student.class));
    }

    @Test
    @DisplayName("非法学习状态应在服务层再次拒绝")
    void shouldRejectInvalidStatus() {
        Student student = student(7L, Student.TEMPORARY);
        when(loginMapper.selectById(7L)).thenReturn(student);
        when(loginMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> service.updateStudent(7L, updateReq(null, "UNKNOWN")))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getCode()).isEqualTo(BusinessException.PARAMS_ERROR.getCode()));

        verify(loginMapper, never()).updateById(any(Student.class));
    }

    @Test
    @DisplayName("办理退学应保留学员并立即终止登录会话")
    void shouldWithdrawStudentAndEvictSession() {
        Student student = student(7L, Student.AT_SCHOOL);
        when(loginMapper.selectById(7L)).thenReturn(student);
        when(loginMapper.updateById(student)).thenReturn(1);

        service.withdrawStudent(7L);

        assertThat(student.getStatus()).isEqualTo(Student.WITCHDRAWAL);
        verify(loginMapper).updateById(student);
        verify(loginMapper, never()).deleteById(eq(7L));
        verify(redisTemplate).delete(TokenConstants.STUDENT_JWT_KEY_PREFIX + 7L);
    }

    @Test
    @DisplayName("重复办理退学应保持幂等并继续清理会话")
    void shouldKeepRepeatedWithdrawalIdempotent() {
        Student student = student(7L, Student.WITCHDRAWAL);
        when(loginMapper.selectById(7L)).thenReturn(student);

        service.withdrawStudent(7L);

        verify(loginMapper, never()).updateById(any(Student.class));
        verify(redisTemplate).delete(TokenConstants.STUDENT_JWT_KEY_PREFIX + 7L);
    }

    @Test
    @DisplayName("班级选项应转换为轻量响应")
    void shouldListClassOptions() {
        when(sysClassService.list(any(Wrapper.class))).thenReturn(List.of(
                sysClass(10L, "Java第19期"),
                sysClass(9L, "Java第18期")
        ));

        List<StudentClassOptionRes> result = service.listClassOptions();

        assertThat(result).containsExactly(
                new StudentClassOptionRes(10L, "Java第19期"),
                new StudentClassOptionRes(9L, "Java第18期")
        );
    }

    private StudentUpdateReq updateReq(Long classId, String status) {
        return new StudentUpdateReq("张三", "13800138000", "student@example.com", classId, status);
    }

    private Student student(Long id, String status) {
        Student student = new Student();
        student.setId(id);
        student.setName("张三");
        student.setPhone("13900139000");
        student.setEmail("old@example.com");
        student.setStatus(status);
        student.setCreatedUserId(1L);
        student.setCreatedAt(LocalDateTime.of(2026, 7, 1, 9, 0));
        student.setUpdatedAt(LocalDateTime.of(2026, 7, 1, 9, 0));
        return student;
    }

    private SysClass sysClass(Long id, String className) {
        SysClass sysClass = new SysClass();
        sysClass.setId(id);
        sysClass.setClassPeriod(className);
        return sysClass;
    }
}
