package com.swordshield.common;

/**
 * 错误码枚举类，定义了系统中常见的错误码和对应的HTTP状态码
 */
public enum ErrorCode {
    OK(0, "ok"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    BAD_REQUEST(400, "bad request"),
    NOT_FOUND(404, "not found"),
    TOO_MANY_REQUESTS(429, "too many requests"),
    BAD_GATEWAY(502, "bad gateway"),
    SERVICE_UNAVAILABLE(503, "service unavailable"),
    GATEWAY_TIMEOUT(504, "gateway timeout"),
    SERVER_ERROR(500, "server error");

    public final int code;
    public final String message;

    /**
     * 构造函数
     * @param code 错误码
     * @param message 错误消息
     */
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据HTTP状态码获取对应的错误码枚举
     * @param status HTTP状态码
     * @return 对应的错误码枚举，如果未找到则根据状态码范围返回BAD_REQUEST或SERVER_ERROR
     */
    public static ErrorCode fromHttpStatus(int status) {
        // 遍历所有错误码枚举值，查找匹配的状态码
        for (ErrorCode value : values()) {
            if (value.code == status) {
                return value;
            }
        }
        // 如果未找到匹配的错误码，根据状态码范围返回默认错误码
        return status >= 400 && status < 500 ? BAD_REQUEST : SERVER_ERROR;
    }
}

