package com.swordshield.common;

import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT工具类，提供JWT令牌的生成和解析功能
 */
public class JwtUtil {
    private static final String DEFAULT_SECRET = "swordshield-dev-secret-change-me";
    private static final Key KEY = Keys.hmacShaKeyFor(getSecret().getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRE_MS = 3600_000;

    /**
     * 获取JWT密钥
     * 优先级：环境变量JWT_SECRET > 系统属性jwt.secret > 默认密钥
     *
     * @return 用于JWT签名的密钥字符串
     */
    private static String getSecret() {
        String env = System.getenv("JWT_SECRET");
        if (env != null && !env.isBlank()) return env;
        String prop = System.getProperty("jwt.secret");
        if (prop != null && !prop.isBlank()) return prop;
        return DEFAULT_SECRET;
    }

    /**
     * 生成JWT令牌
     *
     * @param subject 令牌的主题，通常为用户标识
     * @return 生成的JWT令牌字符串
     */
    public static String generateToken(String subject) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + EXPIRE_MS))
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析JWT令牌并获取主题信息
     *
     * @param token JWT令牌字符串
     * @return 令牌中的主题信息
     */
    public static String parseSubject(String token) {
        return Jwts.parserBuilder().setSigningKey(KEY).build()
                .parseClaimsJws(token).getBody().getSubject();
    }
}

