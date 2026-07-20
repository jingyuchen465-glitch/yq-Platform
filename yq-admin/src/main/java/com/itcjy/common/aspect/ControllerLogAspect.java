package com.itcjy.common.aspect;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Controller层统一日志切面
 * 拦截所有Controller方法，记录请求参数、响应结果及耗时，并对敏感字段进行脱敏处理
 */
@Slf4j
@Aspect
@Component
public class ControllerLogAspect {

    /** 敏感字段集合，日志输出时这些字段的值会被替换为 ****** */
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

    /** 切入点：匹配 com.itcjy 包下所有 controller 子包中的 public 方法 */
    @Pointcut("execution(public * com.itcjy..controller..*(..))")
    public void controllerMethods() {
    }

    /** 环绕通知：在Controller方法执行前后记录日志 */
    @Around("controllerMethods()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录开始时间，用于计算耗时
        long startTime = System.currentTimeMillis();
        // 获取当前HTTP请求对象
        HttpServletRequest request = getRequest();
        // 获取方法签名，拼接 "类全限定名.方法名" 作为处理器标识
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String handler = signature.getDeclaringTypeName() + "." + signature.getName();
        // 构建完整请求URL（含查询参数）
        String url = buildUrl(request);
        // 获取HTTP请求方法（GET/POST等）
        String httpMethod = request == null ? "UNKNOWN" : request.getMethod();
        // 将方法入参转为JSON字符串（已过滤不可序列化参数，已脱敏）
        String params = toJson(buildParamMap(signature.getParameterNames(), joinPoint.getArgs()));

        // 打印请求入参日志
        log.info("Controller request: url={}, method={}, handler={}, params={}", url, httpMethod, handler, params);
        try {
            // 执行目标Controller方法
            Object result = joinPoint.proceed();
            // 计算耗时并打印响应日志
            long cost = System.currentTimeMillis() - startTime;
            log.info("Controller response: url={}, method={}, handler={}, cost={}ms, result={}",
                    url, httpMethod, handler, cost, toJson(result));
            return result;
        } catch (Throwable ex) {
            // 发生异常时记录错误日志（含异常堆栈），然后继续抛出
            long cost = System.currentTimeMillis() - startTime;
            log.error("Controller exception: url={}, method={}, handler={}, cost={}ms, params={}",
                    url, httpMethod, handler, cost, params, ex);
            throw ex;
        }
    }

    /** 从Spring上下文中获取当前线程绑定的HttpServletRequest */
    private HttpServletRequest getRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /** 构建完整URL：URI + 查询字符串（如果有） */
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

    /** 构建参数Map：将方法参数名与参数值配对，过滤掉不可日志化的参数 */
    private Map<String, Object> buildParamMap(String[] parameterNames, Object[] args) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (args == null || args.length == 0) {
            return params;
        }
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            // 跳过不可序列化的参数（如Request、Response、文件上传等）
            if (!isLoggable(arg)) {
                continue;
            }
            // 若参数名不可用则使用 arg0、arg1 作为兜底名称
            String paramName = parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i;
            params.put(paramName, arg);
        }
        return params;
    }

    /** 判断参数是否可日志化：排除Request、Response、文件、校验结果等无法/无需序列化的类型 */
    private boolean isLoggable(Object arg) {
        return !(arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof MultipartFile
                || arg instanceof BindingResult);
    }

    /** 将对象转为JSON字符串，转换失败时降级为 toString 输出 */
    private String toJson(Object value) {
        try {
            Object jsonValue = JSON.toJSON(value);
            // 对敏感字段进行脱敏
            maskSensitiveValue(jsonValue);
            return JSON.toJSONString(jsonValue);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    /** 递归遍历JSON结构，将敏感字段的值替换为 "******" */
    private void maskSensitiveValue(Object value) {
        if (value instanceof JSONObject jsonObject) {
            for (String key : jsonObject.keySet()) {
                Object item = jsonObject.get(key);
                if (isSensitiveKey(key)) {
                    // 命中敏感key，直接脱敏
                    jsonObject.put(key, "******");
                } else {
                    // 未命中则递归处理嵌套对象
                    maskSensitiveValue(item);
                }
            }
            return;
        }
        if (value instanceof JSONArray jsonArray) {
            // 数组中逐个元素递归脱敏
            for (Object item : jsonArray) {
                maskSensitiveValue(item);
            }
        }
    }

    /** 判断key是否为敏感字段（忽略大小写） */
    private boolean isSensitiveKey(String key) {
        return SENSITIVE_KEYS.stream().anyMatch(sensitiveKey -> sensitiveKey.equalsIgnoreCase(key));
    }
}
