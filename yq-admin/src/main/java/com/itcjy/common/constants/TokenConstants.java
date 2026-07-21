package com.itcjy.common.constants;

import java.security.SecureRandom;
import java.time.Duration;

public class TokenConstants {
    public static final String TOKEN_PREFIX = "Bearer ";
    //过期时间5min
    public static final long EXPIRATION_TIME = 5 * 60 * 1000;

    // 密码学安全的随机数生成器，用于生成签名密钥
    public static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static final String USER_JWT_KEY_PREFIX = "user_jwt_key_";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHORIZATION = "Authorization";

    /** 签名请求有效期（5分钟，超过则视为过期） */
    public static final Duration SIGN_REQUEST_EXPIRE_DURATION = Duration.ofMinutes(5);

    /** nonce 防重放 Redis key 前缀 */
    private static final String SIGN_NONCE_KEY_PREFIX = "sign_nonce:";

    /**
     * 生成 nonce 防重放的 Redis key
     */
    public static String signNonceKey(String nonce) {
        return SIGN_NONCE_KEY_PREFIX + nonce;
    }
}
