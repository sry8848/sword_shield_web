package com.swordshield.common;

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

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ErrorCode fromHttpStatus(int status) {
        for (ErrorCode value : values()) {
            if (value.code == status) {
                return value;
            }
        }
        return status >= 400 && status < 500 ? BAD_REQUEST : SERVER_ERROR;
    }
}

