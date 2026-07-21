package com.itcjy.config;

import com.itcjy.common.interceptor.JwtAuthInterceptor;
import com.itcjy.common.interceptor.PermissionInterceptor;
import com.itcjy.common.interceptor.SignInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {


    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Autowired
    private SignInterceptor signInterceptor;

    @Autowired
    private PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截器按注册顺序执行：先解析 JWT 得到 userId，再验签，最后做接口权限判断。
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/emp/**")
                .excludePathPatterns(
                        "/emp/sysUser/login",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**");

        registry.addInterceptor(signInterceptor)
                .addPathPatterns("/emp/**")
                .excludePathPatterns(
                        "/emp/sysUser/login",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**");

        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/emp/**")
                .excludePathPatterns(
                        "/emp/sysUser/login",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**");

        WebMvcConfigurer.super.addInterceptors(registry);
    }
}
