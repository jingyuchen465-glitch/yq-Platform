package com.itcjy.common.aspect;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.itcjy.common.filter.RequestIdFilter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Controller 层统一日志切面
 * <p>
 * 拦截所有 Controller 方法，记录请求入参、响应结果及耗时，
 * 并对敏感字段（密码、令牌等）进行脱敏处理，避免敏感信息泄露到日志中。
 * </p>
 *
 * @author itcjy
 */
@Slf4j
@Aspect
@Component
public class ControllerLogAspect {

    /** 需要脱敏的敏感字段名集合（忽略大小写匹配） */
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "oldPassword",
            "newPassword",
            "confirmPassword",
            "token",
            "accessToken",
            "refreshToken",
            "authorization"
    );

    /**
     * 切入点：匹配 com.itcjy 包及子包下所有 controller 类的 public 方法
     */
    @Pointcut("execution(public * com.itcjy..controller..*(..))")
    public void controllerMethods() {
    }

    /**
     * 环绕通知：在 Controller 方法执行前后记录日志
     * <ul>
     *     <li>执行前：记录 requestId、URL、HTTP 方法、处理方法、请求参数</li>
     *     <li>执行后：记录响应结果及耗时</li>
     *     <li>异常时：记录异常信息及耗时</li>
     * </ul>
     *
     * @param joinPoint 连接点
     * @return Controller 方法的返回值
     * @throws Throwable 方法执行过程中抛出的异常
     */
    @Around("controllerMethods()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录开始时间，用于计算耗时
        long startTime = System.currentTimeMillis();
        // 获取当前 HTTP 请求对象
        HttpServletRequest request = getRequest();
        // 获取方法签名信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取请求追踪 ID（由 RequestIdFilter 写入 MDC）
        String requestId = getRequestId();
        // 拼接处理方法全限定名：类名.方法名
        String handler = signature.getDeclaringTypeName() + "." + signature.getName();
        // 构建完整请求 URL（含查询参数）
        String url = buildUrl(request);
        // 获取 HTTP 请求方法（GET/POST/PUT/DELETE 等）
        String httpMethod = request == null ? "UNKNOWN" : request.getMethod();
        // 将方法入参序列化为 JSON（已过滤不可序列化参数并脱敏）
        String params = toJson(buildParamMap(signature.getParameterNames(), joinPoint.getArgs()));

        // 打印请求日志
        log.info("Controller request: requestId={}, url={}, method={}, handler={}, params={}",
                requestId, url, httpMethod, handler, params);
        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - startTime;
            // 打印正常响应日志
            log.info("Controller response: requestId={}, url={}, method={}, handler={}, cost={}ms, result={}",
                    requestId, url, httpMethod, handler, cost, toJson(result));
            return result;
        } catch (Throwable ex) {
            long cost = System.currentTimeMillis() - startTime;
            // 打印异常日志
            log.error("Controller exception: requestId={}, url={}, method={}, handler={}, cost={}ms, params={}",
                    requestId, url, httpMethod, handler, cost, params, ex);
            throw ex;
        }
    }

    /**
     * 从 MDC 中获取请求追踪 ID
     *
     * @return requestId，若不存在则返回 "UNKNOWN"
     */
    private String getRequestId() {
        String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY);
        return requestId == null || requestId.isBlank() ? "UNKNOWN" : requestId;
    }

    /**
     * 从 Spring 上下文中获取当前 HttpServletRequest 对象
     *
     * @return 当前请求对象，非 Web 环境下返回 null
     */
    private HttpServletRequest getRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 构建完整请求 URL（URI + 查询字符串）
     *
     * @param request HTTP 请求对象
     * @return 完整 URL 字符串，request 为 null 时返回 "UNKNOWN"
     */
    private String buildUrl(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isBlank()) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + queryString;
    }

    /**
     * 构建方法参数映射（参数名 -> 参数值），过滤不可日志化的参数
     *
     * @param parameterNames 方法参数名数组
     * @param args           方法参数值数组
     * @return 可日志化的参数键值对（保持插入顺序）
     */
    private Map<String, Object> buildParamMap(String[] parameterNames, Object[] args) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (args == null || args.length == 0) {
            return params;
        }
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            // 跳过不可序列化的参数（如 Request、Response、文件等）
            if (!isLoggable(arg)) {
                continue;
            }
            // 若参数名不可用则使用 arg0、arg1 等占位
            String paramName = parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i;
            params.put(paramName, arg);
        }
        return params;
    }

    /**
     * 判断参数是否可日志化（排除 Servlet 对象、文件上传、绑定结果等）
     *
     * @param arg 方法参数
     * @return true 表示可以记录到日志中
     */
    private boolean isLoggable(Object arg) {
        return !(arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof MultipartFile
                || arg instanceof BindingResult);
    }

    /**
     * 将对象序列化为 JSON 字符串，并对敏感字段进行脱敏
     *
     * @param value 待序列化的对象
     * @return 脱敏后的 JSON 字符串；序列化失败时返回 toString 结果
     */
    private String toJson(Object value) {
        try {
            Object jsonValue = JSON.toJSON(value);
            // 递归脱敏敏感字段
            maskSensitiveValue(jsonValue);
            return JSON.toJSONString(jsonValue);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    /**
     * 递归遍历 JSON 结构，将敏感字段的值替换为 "******"
     *
     * @param value JSON 对象或数组
     */
    private void maskSensitiveValue(Object value) {
        if (value instanceof JSONObject jsonObject) {
            for (String key : jsonObject.keySet()) {
                Object item = jsonObject.get(key);
                if (isSensitiveKey(key)) {
                    // 敏感字段直接替换为掩码
                    jsonObject.put(key, "******");
                } else {
                    // 非敏感字段继续递归检查嵌套结构
                    maskSensitiveValue(item);
                }
            }
            return;
        }
        if (value instanceof JSONArray jsonArray) {
            for (Object item : jsonArray) {
                maskSensitiveValue(item);
            }
        }
    }

    /**
     * 判断字段名是否为敏感字段（忽略大小写）
     *
     * @param key 字段名
     * @return true 表示是敏感字段，需要脱敏
     */
    private boolean isSensitiveKey(String key) {
        return SENSITIVE_KEYS.stream().anyMatch(sensitiveKey -> sensitiveKey.equalsIgnoreCase(key));
    }
}
