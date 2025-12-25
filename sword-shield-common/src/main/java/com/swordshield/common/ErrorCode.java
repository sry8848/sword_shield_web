package com.swordshield.common;

public enum ErrorCode {
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    BAD_REQUEST(400, "bad request"),
    NOT_FOUND(404, "not found"),
    SERVER_ERROR(500, "server error");

    public final int code;
    public final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

