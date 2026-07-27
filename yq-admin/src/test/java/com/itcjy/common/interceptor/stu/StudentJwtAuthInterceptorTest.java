package com.itcjy.common.interceptor.stu;

import com.itcjy.common.constants.TokenConstants;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.interceptor.AuthThreadlocal;
import com.itcjy.common.properties.JwtProperties;
import com.itcjy.common.utils.JwtUtil;
import com.itcjy.stu.pojo.VO.LoginInfo;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentJwtAuthInterceptorTest {

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private JwtUtil jwtUtil;
    private StudentJwtAuthInterceptor interceptor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-jwt-secret-that-is-at-least-32-bytes-long");
        jwtUtil = new JwtUtil(properties);
        interceptor = new StudentJwtAuthInterceptor(redisTemplate, jwtUtil);
    }

    @AfterEach
    void cleanThreadLocal() {
        AuthThreadlocal.remove();
    }

    @Test
    @DisplayName("学生令牌和 Redis 会话一致时应建立当前登录会话")
    void shouldAuthenticateCurrentStudent() throws Exception {
        String token = studentToken(7L);
        LoginInfo loginInfo = studentLoginInfo(7L, token);
        when(valueOperations.get(TokenConstants.STUDENT_JWT_KEY_PREFIX + 7L)).thenReturn(loginInfo);

        boolean accepted = interceptor.preHandle(requestWithToken(token), new MockHttpServletResponse(), new Object());

        assertThat(accepted).isTrue();
        assertThat(AuthThreadlocal.getLoginInfo()).isSameAs(loginInfo);
    }

    @Test
    @DisplayName("重新登录后旧学生令牌应失效")
    void shouldRejectStaleTokenAfterRelogin() {
        String staleToken = studentToken(7L);
        LoginInfo currentLogin = studentLoginInfo(7L, "new-login-token");
        when(valueOperations.get(TokenConstants.STUDENT_JWT_KEY_PREFIX + 7L)).thenReturn(currentLogin);

        assertThatThrownBy(() -> interceptor.preHandle(
                requestWithToken(staleToken), new MockHttpServletResponse(), new Object()))
                .isSameAs(BusinessException.USER_NO_TOKEN);
    }

    @Test
    @DisplayName("学员会话被管理端清理后原令牌应立即失效")
    void shouldRejectTokenAfterSessionIsEvicted() {
        String token = studentToken(7L);
        when(valueOperations.get(TokenConstants.STUDENT_JWT_KEY_PREFIX + 7L)).thenReturn(null);

        assertThatThrownBy(() -> interceptor.preHandle(
                requestWithToken(token), new MockHttpServletResponse(), new Object()))
                .isSameAs(BusinessException.USER_NO_TOKEN);
    }

    @Test
    @DisplayName("员工令牌不能访问学生接口")
    void shouldRejectEmployeeTokenOnStudentEndpoint() {
        String employeeToken = jwtUtil.generateToken(7L, "admin", Collections.emptyList());

        assertThatThrownBy(() -> interceptor.preHandle(
                requestWithToken(employeeToken), new MockHttpServletResponse(), new Object()))
                .isSameAs(BusinessException.JWT_ERROR);
    }

    private String studentToken(Long studentId) {
        return jwtUtil.generateToken(
                studentId,
                "13800138000",
                Collections.emptyList(),
                TokenConstants.PRINCIPAL_STUDENT
        );
    }

    private LoginInfo studentLoginInfo(Long studentId, String token) {
        StudentDetailsVO details = new StudentDetailsVO();
        details.setId(studentId);
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setToken(token);
        loginInfo.setSignSecret("student-sign-secret");
        loginInfo.setStudentDetailsVO(details);
        return loginInfo;
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stu/me");
        request.addHeader(TokenConstants.AUTHORIZATION, TokenConstants.BEARER_PREFIX + token);
        return request;
    }
}
