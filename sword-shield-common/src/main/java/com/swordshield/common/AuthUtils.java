package com.swordshield.common;

/**
 * 认证工具类，提供JWT令牌解析、用户身份验证、管理员权限验证等功能
 * 支持从Authorization头或网关注入的头信息中提取用户信息
 */
public final class AuthUtils {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_PREFIX = "admin_";

    private AuthUtils() {}

    /**
     * 从Authorization头中提取Bearer令牌
     *
     * @param authorization Authorization头的值
     * @return 提取的令牌字符串
     * @throws SecurityException 当Authorization头为空、不以Bearer开头或令牌为空时抛出异常
     */
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

    /**
     * 从Authorization头中提取JWT令牌的主题信息
     *
     * @param authorization Authorization头的值
     * @return JWT令牌的主题信息
     * @throws SecurityException 当令牌无效或主题信息为空时抛出异常
     */
    public static String requireSubject(String authorization) {
        String token = extractBearerToken(authorization);
        return JwtUtil.parseSubject(token);
    }

    /**
     * 优先从网关注入的头信息中获取主题信息，如果为空则从JWT Authorization头中解析
     *
     * @param subjectHeader 网关注入的主题头信息（如X-Auth-Subject）
     * @param authorization Authorization头的值
     * @return 主题信息字符串
     * @throws SecurityException 当所有来源的主题信息都为空时抛出异常
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

    /**
     * 从Authorization头中提取用户ID
     *
     * @param authorization Authorization头的值
     * @return 用户ID
     * @throws SecurityException 当令牌无效或用户ID格式错误时抛出异常
     */
    public static long requireUserId(String authorization) {
        String subject = requireSubject(authorization);
        if (subject == null || subject.isBlank()) {
            throw new SecurityException("未登录，请先登录");
        }
        return Long.parseLong(subject);
    }

    /**
     * 优先从网关注入的头信息中获取用户ID，如果为空则从JWT Authorization头中解析
     *
     * @param userIdHeader 网关注入的用户ID头信息（如X-User-Id）
     * @param authorization Authorization头的值
     * @return 用户ID
     * @throws SecurityException 当所有来源的用户ID都无效时抛出异常
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

    /**
     * 从Authorization头中提取用户ID，支持管理员令牌（以admin_前缀开头）
     *
     * @param authorization Authorization头的值
     * @return 用户ID（如果是管理员令牌，则返回去掉admin_前缀后的ID）
     * @throws SecurityException 当令牌无效或用户ID格式错误时抛出异常
     */
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
     * 优先从网关注入的头信息中获取用户ID，支持管理员令牌，如果为空则从JWT Authorization头中解析
     *
     * @param userIdHeader 网关注入的用户ID头信息（如X-User-Id）
     * @param authorization Authorization头的值
     * @return 用户ID
     * @throws SecurityException 当所有来源的用户ID都无效时抛出异常
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

    /**
     * 从Authorization头中提取管理员主题信息，验证用户是否为管理员
     *
     * @param authorization Authorization头的值
     * @return 管理员主题信息
     * @throws SecurityException 当令牌无效时抛出异常
     * @throws ForbiddenException 当用户不是管理员时抛出异常
     */
    public static String requireAdminSubject(String authorization) {
        String subject = requireSubject(authorization);
        if (subject == null || subject.isBlank()) {
            throw new SecurityException("未登录，请先登录");
        }
        if (!subject.startsWith(ADMIN_PREFIX)) {
            throw new ForbiddenException("无权限访问，需要管理员权限");
        }
        return subject;
    }

    /**
     * 优先从网关注入的头信息中获取管理员主题信息，如果为空则从JWT Authorization头中解析
     *
     * @param subjectHeader 网关注入的主题头信息（如X-Auth-Subject）
     * @param authorization Authorization头的值
     * @return 管理员主题信息
     * @throws SecurityException 当令牌无效时抛出异常
     * @throws ForbiddenException 当用户不是管理员时抛出异常
     */
    public static String requireAdminSubject(String subjectHeader, String authorization) {
        String subject = requireSubject(subjectHeader, authorization);
        if (subject == null || subject.isBlank()) {
            throw new SecurityException("未登录，请先登录");
        }
        if (!subject.startsWith(ADMIN_PREFIX)) {
            throw new ForbiddenException("无权限访问，需要管理员权限");
        }
        return subject;
    }

    /**
     * 从Authorization头中提取管理员ID
     *
     * @param authorization Authorization头的值
     * @return 管理员ID
     * @throws SecurityException 当令牌无效或管理员ID格式错误时抛出异常
     */
    public static long requireAdminId(String authorization) {
        String subject = requireAdminSubject(authorization);
        return Long.parseLong(subject.substring(ADMIN_PREFIX.length()));
    }

    /**
     * 从网关注入的头信息或JWT Authorization头中提取管理员ID
     *
     * @param subjectHeader 网关注入的主题头信息（如X-Auth-Subject）
     * @param authorization Authorization头的值
     * @return 管理员ID
     * @throws SecurityException 当令牌无效或管理员ID格式错误时抛出异常
     */
    public static long requireAdminId(String subjectHeader, String authorization) {
        String subject = requireAdminSubject(subjectHeader, authorization);
        return parseLongOrThrow(subject.substring(ADMIN_PREFIX.length()), "无权限访问，需要管理员权限");
    }

    /**
     * 验证Authorization头中的Bearer令牌是否有效
     *
     * @param authorization Authorization头的值
     * @return 令牌是否有效
     */
    public static boolean isValidBearerToken(String authorization) {
        try {
            requireSubject(authorization);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 将字符串解析为长整型，解析失败时抛出异常
     *
     * @param value 要解析的字符串
     * @param message 解析失败时的错误消息
     * @return 解析后的长整型值
     * @throws SecurityException 当解析失败时抛出异常
     */
    private static long parseLongOrThrow(String value, String message) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            throw new SecurityException(message);
        }
    }
}
