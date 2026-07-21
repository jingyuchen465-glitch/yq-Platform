package com.itcjy.common.constants;

import java.security.SecureRandom;

public class TokenConstants {
    public static final String TOKEN_PREFIX = "Bearer ";
    //过期时间5min
    public static final long EXPIRATION_TIME = 5 * 60 * 1000;

    // 密码学安全的随机数生成器，用于生成签名密钥
    public static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static final String USER_JWT_KEY_PREFIX = "user_jwt_key_";
    
}
