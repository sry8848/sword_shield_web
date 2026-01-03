package com.swordshield.common;

/**
 * 远程服务异常类，继承自RuntimeException
 * 用于表示远程服务调用过程中发生的异常，并包含错误码信息
 */
public class RemoteServiceException extends RuntimeException {
    private final int code;

    /**
     * 构造函数，创建远程服务异常实例
     * @param code 错误码，用于标识异常类型
     * @param message 异常描述信息
     */
    public RemoteServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取错误码
     * @return 错误码
     */
    public int getCode() {
        return code;
    }
}

