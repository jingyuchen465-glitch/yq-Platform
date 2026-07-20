package com.itcjy.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求ID过滤器：为每个HTTP请求生成或透传唯一的 requestId
 * 将 requestId 写入 MDC，供全链路日志追踪使用，并在响应头中回传给调用方
 * 优先级设为最高，确保后续所有Filter/Interceptor/Controller都能获取到 requestId
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    /** 请求/响应头中携带 requestId 的 Header 名称 */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    /** MDC 中存储 requestId 的 key，日志格式中可通过 %X{requestId} 引用 */
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    /**
     * 核心过滤逻辑：
     * 1. 获取或生成 requestId
     * 2. 放入 MDC 供日志输出
     * 3. 设置到响应头供调用方追踪
     * 4. 请求结束后清理 MDC，防止线程池复用导致数据串扰
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 获取或生成唯一请求ID
        String requestId = getOrCreateRequestId(request);
        // 写入MDC，后续日志自动携带该requestId，MDC.put() 把 requestId 绑定到当前线程
        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        // 在响应头中回传requestId，方便调用方进行链路追踪
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            // 继续执行过滤器链
            filterChain.doFilter(request, response);
        } finally {
            // 无论请求是否异常，都必须清理MDC，避免线程复用时数据污染
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }

    /**
     * 获取或创建 requestId：
     * 优先从请求头 X-Request-Id 中获取（支持上游服务透传），
     * 若不存在则生成格式为 "req-" + 16位随机字符 的唯一ID
     */
    private String getOrCreateRequestId(HttpServletRequest request) {
        // 尝试从请求头获取上游传递的 requestId
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId != null && !requestId.isBlank()) {
            return requestId.trim();
        }
        // 请求头中无 requestId，生成新的：req- + UUID前16位（去掉横杠）
        return "req-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
