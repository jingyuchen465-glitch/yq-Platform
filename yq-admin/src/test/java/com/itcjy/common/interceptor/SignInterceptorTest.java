package com.itcjy.common.interceptor;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.itcjy.common.constants.TokenConstants;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.stu.pojo.VO.LoginInfo;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignInterceptorTest {

    private static final String SECRET = "student-sign-secret";

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private SignInterceptor interceptor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        interceptor = new SignInterceptor(redisTemplate);

        StudentDetailsVO student = new StudentDetailsVO();
        student.setId(9L);
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setSignSecret(SECRET);
        loginInfo.setStudentDetailsVO(student);
        AuthThreadlocal.setLoginInfo(loginInfo);
    }

    @AfterEach
    void cleanThreadLocal() {
        AuthThreadlocal.remove();
    }

    @Test
    @DisplayName("公共签名拦截器应接受学生会话的合法签名")
    void shouldAcceptValidStudentSignature() throws Exception {
        String nonce = "nonce-1";
        MockHttpServletRequest request = signedRequest(System.currentTimeMillis(), nonce);
        when(valueOperations.setIfAbsent(
                eq(TokenConstants.signNonceKey(TokenConstants.PRINCIPAL_STUDENT, 9L, nonce)),
                eq("1"),
                any(Duration.class)
        )).thenReturn(true);

        boolean accepted = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(accepted).isTrue();
    }

    @Test
    @DisplayName("超过允许时钟偏差的未来请求应被拒绝且不占用 nonce")
    void shouldRejectFarFutureTimestampWithoutConsumingNonce() {
        long future = System.currentTimeMillis()
                + TokenConstants.SIGN_REQUEST_EXPIRE_DURATION.toMillis()
                + 1_000;

        assertThatThrownBy(() -> interceptor.preHandle(
                signedRequest(future, "future-nonce"),
                new MockHttpServletResponse(),
                new Object()
        )).isSameAs(BusinessException.SIGN_EXPIRE);

        verify(valueOperations, never()).setIfAbsent(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("允许范围内的未来时间戳应让 nonce 保留到请求真正失效")
    void shouldKeepFutureNonceUntilRequestExpires() throws Exception {
        String nonce = "future-valid-nonce";
        long timestamp = System.currentTimeMillis() + Duration.ofMinutes(4).toMillis();
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(true);

        interceptor.preHandle(signedRequest(timestamp, nonce), new MockHttpServletResponse(), new Object());

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).setIfAbsent(
                eq(TokenConstants.signNonceKey(TokenConstants.PRINCIPAL_STUDENT, 9L, nonce)),
                eq("1"),
                ttlCaptor.capture()
        );
        assertThat(ttlCaptor.getValue())
                .isGreaterThan(Duration.ofMinutes(8))
                .isLessThanOrEqualTo(Duration.ofMinutes(9));
    }

    private MockHttpServletRequest signedRequest(long timestamp, String nonce) {
        String timestampText = String.valueOf(timestamp);
        String uri = "/yq-admin/stu/me";
        String source = "GET\n" + uri + "\n\n" + timestampText + "\n" + nonce;
        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, SECRET.getBytes(StandardCharsets.UTF_8));
        String sign = hMac.digestBase64(source, StandardCharsets.UTF_8, false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.addHeader("X-Timestamp", timestampText);
        request.addHeader("X-Nonce", nonce);
        request.addHeader("X-Sign", sign);
        return request;
    }
}
