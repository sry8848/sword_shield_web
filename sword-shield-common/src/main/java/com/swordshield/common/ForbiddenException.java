package com.swordshield.common;

/**
 * 禁止访问异常类
 * 用于表示用户没有权限访问某个资源或执行某个操作的运行时异常
 */
public class ForbiddenException extends RuntimeException {
    /**
     * 构造函数
     * @param message 异常描述信息
     */
    public ForbiddenException(String message) {
        super(message);
    }
}

