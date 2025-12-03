package com.swordshield.common;

import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

public class JwtUtil {
    private static final String DEFAULT_SECRET = "swordshield-dev-secret-change-me";
    private static final Key KEY = Keys.hmacShaKeyFor(getSecret().getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRE_MS = 3600_000;

    private static String getSecret() {
        String env = System.getenv("JWT_SECRET");
        if (env != null && !env.isBlank()) return env;
        String prop = System.getProperty("jwt.secret");
        if (prop != null && !prop.isBlank()) return prop;
        return DEFAULT_SECRET;
    }

    public static String generateToken(String subject) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + EXPIRE_MS))
                .signWith(KEY)
                .compact();
    }

    public static String parseSubject(String token) {
        return Jwts.parserBuilder().setSigningKey(KEY).build()
                .parseClaimsJws(token).getBody().getSubject();
    }
}

