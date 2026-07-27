package com.itcjy.common.interceptor.stu;

import com.itcjy.common.constants.TokenConstants;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.interceptor.AuthThreadlocal;
import com.itcjy.common.interceptor.LoginSession;
import com.itcjy.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 从前端请求头里面获取Token，解析Token然后判断Redis里面有没有对应的Token
 * 如果有的话就把loginInfo放到ThreadLocal里面
 */
@Component
@RequiredArgsConstructor
public class StudentJwtAuthInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 1. 通过请求头获取 token
        String authorization = request.getHeader(TokenConstants.AUTHORIZATION);

        // 2. 判断 token 是否存在，以及开头必须是 Bearer
        if (authorization == null || !authorization.startsWith(TokenConstants.BEARER_PREFIX)) {
            throw new BusinessException(401, "未登录或者token缺失");
        }

        // 3. 截取 token 值
        String token = authorization.substring(TokenConstants.BEARER_PREFIX.length()).trim();

        // 4. 使用 JwtUtil 验证 token 合法性（签名 + 过期）
        Claims claims;
        try {
            claims = jwtUtil.parseClaims(token);
        } catch (ExpiredJwtException e) {
            throw BusinessException.JWT_EXPIRE;
        } catch (JwtException | IllegalArgumentException e) {
            throw BusinessException.JWT_ERROR;
        }

        // 5. 从 claims 中获取 userId
        Object userIdObj = claims.get(JwtUtil.CLAIM_USER_ID);
        if (userIdObj == null) {
            throw BusinessException.JWT_ERROR;
        }
        Long uid;
        try {
            uid = userIdObj instanceof Number num ? num.longValue() : Long.valueOf(userIdObj.toString());
        } catch (NumberFormatException e) {
            throw BusinessException.JWT_ERROR;
        }
        String principalType = claims.get(JwtUtil.CLAIM_PRINCIPAL_TYPE, String.class);
        if (principalType != null && !TokenConstants.PRINCIPAL_STUDENT.equals(principalType)) {
            throw BusinessException.JWT_ERROR;
        }

        // 6. 拼凑 redis key，判断 redis 是否还存在该 token
        String redisKey = TokenConstants.STUDENT_JWT_KEY_PREFIX + uid;
        Object loginInfoObj = redisTemplate.opsForValue().get(redisKey);
        if (loginInfoObj == null) {
            throw BusinessException.USER_NO_TOKEN;
        }
        if (!(loginInfoObj instanceof LoginSession loginInfo)
                || !uid.equals(loginInfo.getPrincipalId())
                || !TokenConstants.PRINCIPAL_STUDENT.equals(loginInfo.getPrincipalType())
                || !token.equals(loginInfo.getToken())) {
            throw BusinessException.USER_NO_TOKEN;
        }

        // 7. 存储到 ThreadLocal 当中
        AuthThreadlocal.setLoginInfo(loginInfo);

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 响应完成后移除当前线程上的用户信息，异常请求也会执行，避免线程复用导致数据串扰
        AuthThreadlocal.remove();
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}

