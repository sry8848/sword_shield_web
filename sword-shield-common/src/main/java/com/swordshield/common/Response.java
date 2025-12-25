package com.swordshield.common;

public class Response<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Response<T> ok(T data) {
        Response<T> r = new Response<>();
        r.code = ErrorCode.OK.code;
        r.message = ErrorCode.OK.message;
        r.data = data;
        return r;
    }

    public static <T> Response<T> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.message);
    }

    public static <T> Response<T> error(ErrorCode errorCode, String message) {
        return error(errorCode.code, message);
    }

    public static <T> Response<T> error(int code, String message) {
        Response<T> r = new Response<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

