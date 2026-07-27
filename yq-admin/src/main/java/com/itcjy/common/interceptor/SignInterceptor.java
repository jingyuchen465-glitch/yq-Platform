package com.itcjy.common.interceptor;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.itcjy.common.constants.TokenConstants;
import com.itcjy.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

/**
 * 请求签名验证拦截器。防止过期请求重复请求
 * <p>
 * 前端每次请求携带 X-Timestamp、X-Nonce、X-Sign 三个请求头，
 * 服务端使用登录时分配的 signSecret 重新计算签名并比对，防止请求被篡改或重放。
 */
@Component
@RequiredArgsConstructor
public class SignInterceptor implements HandlerInterceptor {

    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGN = "X-Sign";

    /** nonce Redis 缓存值（SETNX 只关心能否存储成功，值本身无意义） */
    private static final String NONCE_CACHE_VALUE = "1";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 1. 获取请求头中的签名信息
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);//前端生成的随机唯一值
        String sign = request.getHeader(HEADER_SIGN);
        if (!StrUtil.isAllNotBlank(timestamp, nonce, sign)) {
            throw BusinessException.REQUEST_HEADER_ERROR;
        }

        // 2. 验证时间戳，计算 nonce 有效期，防止过期请求重复请求
        long timestampMillis = parseTimestamp(timestamp);
        Duration nonceTtl = buildNonceTtl(timestampMillis);

        // 4. 获取当前登录用户的 signSecret
        //AuthThreadlocal.getLoginInfo()就是来着redis里面的值
        LoginSession loginInfo = AuthThreadlocal.getLoginInfo();
        if (loginInfo == null) {
            throw BusinessException.USER_NO_TOKEN;
        }
        String signSecret = loginInfo.getSignSecret();
        if (StrUtil.isBlank(signSecret)) {
            throw BusinessException.SIGN_SECRET_NOT_FOUND;
        }

        // 5. 构建待签名原文并验签
        //5.1.把请求的关键信息拼接成一个字符串（原文）
        String source = buildSignSource(request, timestamp, nonce);
        //5.2.后端用同样的密钥和同样的算法，重新计算一遍签名。
        String serverSign = sign(source, signSecret);
        //5.3.后端算出的签名 和 前端传来的签名 是否一致
        if (!MessageDigest.isEqual(
                serverSign.getBytes(StandardCharsets.UTF_8),
                sign.getBytes(StandardCharsets.UTF_8))) {
            throw BusinessException.SIGN_ERROR;
        }

        // 6. 验签通过后再使用 SETNX 占用 nonce，避免无效签名提前消耗合法 nonce
        Boolean setNonceSuccess = redisTemplate.opsForValue().setIfAbsent(
                TokenConstants.signNonceKey(loginInfo.getPrincipalType(), loginInfo.getPrincipalId(), nonce),
                NONCE_CACHE_VALUE,
                nonceTtl
        );
        if (!Boolean.TRUE.equals(setNonceSuccess)) {
            throw BusinessException.SIGN_NONCE_REPEAT;
        }

        return true;
    }

    /**
     * 解析签名时间戳
     */
    private long parseTimestamp(String timestamp) {
        try {
            return Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw BusinessException.PARSE_TIMESTAMP_ERROR;
        }
    }

    /**
     * 构建 nonce 缓存时长，同时校验请求是否过期
     */
    private Duration buildNonceTtl(long timestampMillis) {
        long now = System.currentTimeMillis();
        long allowedSkew = TokenConstants.SIGN_REQUEST_EXPIRE_DURATION.toMillis();
        if (timestampMillis < now - allowedSkew || timestampMillis > now + allowedSkew) {
            throw BusinessException.SIGN_EXPIRE;
        }
        long expireAt = timestampMillis + allowedSkew;
        return Duration.ofMillis(Math.max(1, expireAt - now));
    }

    /**
     * 构建待签名原文：method + uri + query + timestamp + nonce
     */
    private String buildSignSource(HttpServletRequest request, String timestamp, String nonce) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = ObjUtil.defaultIfNull(request.getQueryString(), StrUtil.EMPTY);
        return method + "\n" + uri + "\n" + query + "\n" + timestamp + "\n" + nonce;
    }

    /**
     * 使用 HMAC-SHA256 计算 Base64 签名
     */
    private String sign(String source, String signSecret) {
        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, signSecret.getBytes(StandardCharsets.UTF_8));
        return hMac.digestBase64(source, StandardCharsets.UTF_8, false);
    }
}
