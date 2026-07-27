package com.itcjy.common.interceptor;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 接口权限校验拦截器。
 * <p>
 * JWT 拦截器会先把当前登录用户信息放入 ThreadLocal，这里读取用户权限列表，
 * 再和 Controller 方法上的 {@link HasPermission#code()} 对比，有权限才放行。
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 静态资源等处理器不是 HandlerMethod，不参与接口权限校验
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        HasPermission hasPermission = handlerMethod.getMethodAnnotation(HasPermission.class);
        if (hasPermission == null) {
            return true;
        }

        LoginSession loginInfo = AuthThreadlocal.getLoginInfo();
        if (loginInfo == null) {
            throw BusinessException.USER_NO_TOKEN;
        }

        List<String> permissions = loginInfo.getPermissions();
        if (permissions == null || !permissions.contains(hasPermission.code())) {
            throw new BusinessException(403, "没有权限访问该接口");
        }

        return true;
    }
}
