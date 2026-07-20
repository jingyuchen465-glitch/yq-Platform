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

@Slf4j
@Aspect
@Component
public class ControllerLogAspect {

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

    @Pointcut("execution(public * com.itcjy..controller..*(..))")
    public void controllerMethods() {
    }

    @Around("controllerMethods()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = getRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String requestId = getRequestId();
        String handler = signature.getDeclaringTypeName() + "." + signature.getName();
        String url = buildUrl(request);
        String httpMethod = request == null ? "UNKNOWN" : request.getMethod();
        String params = toJson(buildParamMap(signature.getParameterNames(), joinPoint.getArgs()));

        log.info("Controller request: requestId={}, url={}, method={}, handler={}, params={}",
                requestId, url, httpMethod, handler, params);
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - startTime;
            log.info("Controller response: requestId={}, url={}, method={}, handler={}, cost={}ms, result={}",
                    requestId, url, httpMethod, handler, cost, toJson(result));
            return result;
        } catch (Throwable ex) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("Controller exception: requestId={}, url={}, method={}, handler={}, cost={}ms, params={}",
                    requestId, url, httpMethod, handler, cost, params, ex);
            throw ex;
        }
    }

    private String getRequestId() {
        String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY);
        return requestId == null || requestId.isBlank() ? "UNKNOWN" : requestId;
    }

    private HttpServletRequest getRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

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

    private Map<String, Object> buildParamMap(String[] parameterNames, Object[] args) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (args == null || args.length == 0) {
            return params;
        }
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (!isLoggable(arg)) {
                continue;
            }
            String paramName = parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i;
            params.put(paramName, arg);
        }
        return params;
    }

    private boolean isLoggable(Object arg) {
        return !(arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof MultipartFile
                || arg instanceof BindingResult);
    }

    private String toJson(Object value) {
        try {
            Object jsonValue = JSON.toJSON(value);
            maskSensitiveValue(jsonValue);
            return JSON.toJSONString(jsonValue);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private void maskSensitiveValue(Object value) {
        if (value instanceof JSONObject jsonObject) {
            for (String key : jsonObject.keySet()) {
                Object item = jsonObject.get(key);
                if (isSensitiveKey(key)) {
                    jsonObject.put(key, "******");
                } else {
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

    private boolean isSensitiveKey(String key) {
        return SENSITIVE_KEYS.stream().anyMatch(sensitiveKey -> sensitiveKey.equalsIgnoreCase(key));
    }
}
