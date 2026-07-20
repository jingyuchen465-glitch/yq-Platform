package com.itcjy;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(title = "yq-admin API", version = "1.0", description = "yq-admin 后台接口文档"))
@SpringBootApplication
public class YqAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(YqAdminApplication.class, args);
    }
}
