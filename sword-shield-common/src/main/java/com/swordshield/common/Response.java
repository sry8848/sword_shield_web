package com.swordshield.common;

/**
 * 通用响应类，用于封装API响应结果
 * @param <T> 响应数据的类型
 */
public class Response<T> {
    private int code;
    private String message;
    private T data;

    /**
     * 创建成功的响应
     * @param data 响应数据
     * @param <T> 数据类型
     * @return Response对象，code为OK，message为OK，data为传入的数据
     */
    public static <T> Response<T> ok(T data) {
        Response<T> r = new Response<>();
        r.code = ErrorCode.OK.code;
        r.message = ErrorCode.OK.message;
        r.data = data;
        return r;
    }

    /**
     * 创建错误响应
     * @param errorCode 错误码枚举
     * @param <T> 数据类型
     * @return Response对象，使用错误码的code和message
     */
    public static <T> Response<T> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.message);
    }

    /**
     * 创建错误响应
     * @param errorCode 错误码枚举
     * @param message 自定义错误消息
     * @param <T> 数据类型
     * @return Response对象，使用错误码的code和自定义message
     */
    public static <T> Response<T> error(ErrorCode errorCode, String message) {
        return error(errorCode.code, message);
    }

    /**
     * 创建错误响应
     * @param code 错误码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return Response对象，使用指定的code和message
     */
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

