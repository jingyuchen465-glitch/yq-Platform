package com.itcjy.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * HS256 secret. Production should override it with JWT_SECRET.
     */
    private String secret;

    /**
     * Token expiration time in milliseconds. Default: 2 hours.
     */
    private Long expireMillis = 2 * 60 * 60 * 1000L;

    /**
     * Token issuer.
     */
    private String issuer = "yq-admin";

    /**
     * Authorization header name.
     */
    private String headerName = "Authorization";

    /**
     * Authorization header prefix.
     */
    private String tokenPrefix = "Bearer ";
}
