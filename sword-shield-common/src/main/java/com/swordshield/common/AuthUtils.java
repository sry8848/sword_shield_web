package com.swordshield.common;

public final class AuthUtils {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_PREFIX = "admin_";

    private AuthUtils() {}

    public static String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank() || !authorization.startsWith(BEARER_PREFIX)) {
            throw new SecurityException("未登录，请先登录");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new SecurityException("未登录，请先登录");
        }
        return token;
    }

    public static String requireSubject(String authorization) {
        String token = extractBearerToken(authorization);
        return JwtUtil.parseSubject(token);
    }

    public static long requireUserId(String authorization) {
        String subject = requireSubject(authorization);
        if (subject == null || subject.isBlank()) {
            throw new SecurityException("未登录，请先登录");
        }
        return Long.parseLong(subject);
    }

    public static long requireUserIdAllowAdmin(String authorization) {
        String subject = requireSubject(authorization);
        if (subject == null || subject.isBlank()) {
            throw new SecurityException("未登录，请先登录");
        }
        if (subject.startsWith(ADMIN_PREFIX)) {
            subject = subject.substring(ADMIN_PREFIX.length());
        }
        return Long.parseLong(subject);
    }

    public static String requireAdminSubject(String authorization) {
        String subject = requireSubject(authorization);
        if (subject == null || subject.isBlank()) {
            throw new SecurityException("未登录，请先登录");
        }
        if (!subject.startsWith(ADMIN_PREFIX)) {
            throw new SecurityException("无权限访问，需要管理员权限");
        }
        return subject;
    }

    public static long requireAdminId(String authorization) {
        String subject = requireAdminSubject(authorization);
        return Long.parseLong(subject.substring(ADMIN_PREFIX.length()));
    }

    public static boolean isValidBearerToken(String authorization) {
        try {
            requireSubject(authorization);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}

