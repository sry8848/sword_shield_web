package com.swordshield.common;

/**
 * 自定义异常类，用于表示客户端请求过于频繁，需要进行退避处理（对应HTTP 429状态码语义）
 * 该异常会被GlobalExceptionHandler映射为ErrorCode.TOO_MANY_REQUESTS错误码
 */
public class TooManyRequestsException extends RuntimeException {
    /**
     * 构造函数，创建带有指定错误消息的TooManyRequestsException实例
     *
     * @param message 异常描述信息，用于说明请求过于频繁的具体原因
     */
    public TooManyRequestsException(String message) {
        super(message);
    }
}

