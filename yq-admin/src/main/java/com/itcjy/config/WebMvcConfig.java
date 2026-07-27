package com.itcjy.config;

import com.itcjy.common.interceptor.JwtAuthInterceptor;
import com.itcjy.common.interceptor.PermissionInterceptor;
import com.itcjy.common.interceptor.SignInterceptor;
import com.itcjy.common.interceptor.stu.StudentJwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final StudentJwtAuthInterceptor studentJwtAuthInterceptor;
    private final SignInterceptor signInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截器按注册顺序执行：先解析 JWT 得到 userId，再验签，最后做接口权限判断。
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/emp/**")
                .excludePathPatterns(
                        "/emp/sysUser/login",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
                        );
        registry.addInterceptor(studentJwtAuthInterceptor)
                .addPathPatterns("/stu/**")
                .excludePathPatterns("/stu/login");

        registry.addInterceptor(signInterceptor)
                .addPathPatterns("/emp/**", "/stu/**")
                .excludePathPatterns(
                        "/emp/sysUser/login",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/stu/login"
                        );

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
