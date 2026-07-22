package com.itcjy.common.interceptor;

import com.itcjy.common.constants.TokenConstants;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.utils.JwtUtil;
import com.itcjy.emp.pojo.res.system.LoginInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 从前端请求头里面获取Token，解析Token然后判断Redis里面有没有对应的Token
 * 如果有的话就把loginInfo放到ThreadLocal里面
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
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
        Long uid = userIdObj instanceof Number num ? num.longValue() : Long.valueOf(userIdObj.toString());

        // 6. 拼凑 redis key，判断 redis 是否还存在该 token
        String redisKey = TokenConstants.USER_JWT_KEY_PREFIX + uid;
        Object loginInfoObj = redisTemplate.opsForValue().get(redisKey);
        if (loginInfoObj == null) {
            throw BusinessException.USER_NO_TOKEN;
        }
        LoginInfo loginInfo = (LoginInfo) loginInfoObj;

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

