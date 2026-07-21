package com.itcjy.common.utils;

import com.itcjy.common.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLES = "roles";

    private static final int HS256_MIN_SECRET_LENGTH = 32;

    private final JwtProperties jwtProperties;

    public String generateToken(Long userId, String username, List<String> roleCodes, long expirationTime) {
        return generateToken(userId, username, Collections.emptyMap());
    }

    public String generateToken(Long userId, String username, Collection<String> roleCodes) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ROLES, roleCodes == null ? Collections.emptyList() : roleCodes);
        return generateToken(userId, username, claims);
    }

    public String generateToken(Long userId, String username, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>();
        if (extraClaims != null) {
            claims.putAll(extraClaims);
        }
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USERNAME, username);
        return generateToken(username, claims, jwtProperties.getExpireMillis());
    }

    public String generateToken(String subject, Map<String, Object> claims, Long expireMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + getExpireMillis(expireMillis));
        return Jwts.builder()
                .setClaims(claims == null ? Collections.emptyMap() : claims)
                .setSubject(subject)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(cleanToken(token))
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isExpired(String token) {
        Date expiration = getExpiration(token);
        return expiration.before(new Date());
    }

    public Long getUserId(String token) {
        Object userId = parseClaims(token).get(CLAIM_USER_ID);
        if (userId == null) {
            return null;
        }
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(userId.toString());
    }

    public String getUsername(String token) {
        Claims claims = parseClaims(token);
        Object username = claims.get(CLAIM_USERNAME);
        return username == null ? claims.getSubject() : username.toString();
    }

    public Date getExpiration(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    public Date getIssuedAt(String token) {
        return getClaim(token, Claims::getIssuedAt);
    }

    public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(parseClaims(token));
    }

    public String resolveToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        String prefix = jwtProperties.getTokenPrefix();
        if (!authorizationHeader.startsWith(prefix)) {
            return null;
        }
        return authorizationHeader.substring(prefix.length()).trim();
    }

    public String buildAuthorizationHeader(String token) {
        return jwtProperties.getTokenPrefix() + cleanToken(token);
    }

    private String cleanToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT token cannot be blank");
        }
        String trimmedToken = token.trim();
        String prefix = jwtProperties.getTokenPrefix();
        if (trimmedToken.startsWith(prefix)) {
            return trimmedToken.substring(prefix.length()).trim();
        }
        return trimmedToken;
    }

    private Long getExpireMillis(Long expireMillis) {
        if (expireMillis == null || expireMillis <= 0) {
            return jwtProperties.getExpireMillis();
        }
        return expireMillis;
    }

    private SecretKey getSecretKey() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < HS256_MIN_SECRET_LENGTH) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
