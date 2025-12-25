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

    /**
     * Prefer subject injected by gateway (e.g. X-Auth-Subject), fallback to parsing JWT Authorization header.
     */
    public static String requireSubject(String subjectHeader, String authorization) {
        if (subjectHeader != null) {
            String subject = subjectHeader.trim();
            if (!subject.isEmpty()) {
                return subject;
            }
        }
        return requireSubject(authorization);
    }

    public static long requireUserId(String authorization) {
        String subject = requireSubject(authorization);
        if (subject == null || subject.isBlank()) {
            throw new SecurityException("未登录，请先登录");
        }
        return Long.parseLong(subject);
    }

    /**
     * Prefer user id injected by gateway (e.g. X-User-Id), fallback to parsing JWT Authorization header.
     */
    public static long requireUserId(String userIdHeader, String authorization) {
        if (userIdHeader != null) {
            String value = userIdHeader.trim();
            if (!value.isEmpty()) {
                return parseLongOrThrow(value, "未登录，请先登录");
            }
        }
        return requireUserId(authorization);
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

    /**
     * Prefer user id injected by gateway (e.g. X-User-Id), fallback to parsing JWT Authorization header.
     * For admin tokens, gateway injects the numeric admin id as X-User-Id.
     */
    public static long requireUserIdAllowAdmin(String userIdHeader, String authorization) {
        if (userIdHeader != null) {
            String value = userIdHeader.trim();
            if (!value.isEmpty()) {
                return parseLongOrThrow(value, "未登录，请先登录");
            }
        }
        return requireUserIdAllowAdmin(authorization);
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

    /**
     * Prefer subject injected by gateway (e.g. X-Auth-Subject), fallback to parsing JWT Authorization header.
     */
    public static String requireAdminSubject(String subjectHeader, String authorization) {
        String subject = requireSubject(subjectHeader, authorization);
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

    public static long requireAdminId(String subjectHeader, String authorization) {
        String subject = requireAdminSubject(subjectHeader, authorization);
        return parseLongOrThrow(subject.substring(ADMIN_PREFIX.length()), "无权限访问，需要管理员权限");
    }

    public static boolean isValidBearerToken(String authorization) {
        try {
            requireSubject(authorization);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static long parseLongOrThrow(String value, String message) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            throw new SecurityException(message);
        }
    }
}
