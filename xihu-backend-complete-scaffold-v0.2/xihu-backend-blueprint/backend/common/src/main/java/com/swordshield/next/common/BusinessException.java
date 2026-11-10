package com.swordshield.next.common;

import java.util.Objects;

/** 表达业务拒绝并保留原始失败原因。 */
public final class BusinessException extends RuntimeException {

    /** 接口边界据此决定如何表达失败类别。 */
    private final FailureKind kind;
    /** 业务模块定义的稳定错误码。 */
    private final String code;

    /**
     * 创建由业务规则直接判定的失败。
     *
     * @param kind 业务失败类别
     * @param code 稳定的业务错误码
     * @param message 可公开的业务说明
     */
    public BusinessException(FailureKind kind, String code, String message) {
        this(kind, code, message, null);
    }

    /**
     * 将已明确业务语义的失败转换为业务异常。
     *
     * @param kind 业务失败类别
     * @param code 稳定的业务错误码
     * @param message 可公开的业务说明
     * @param cause 可选的原始异常
     */
    public BusinessException(FailureKind kind, String code, String message, Throwable cause) {
        // 诊断信息保留在原因链中，公开说明可独立提供。
        super(message, cause);

        // 1. 校验业务失败类别、错误码和公开说明。
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

        // 2. 保存业务模块提供的错误码。
        this.code = code;
    }

    /** 返回业务失败类别。 */
    public FailureKind getKind() {
        return kind;
    }

    /** 返回稳定的业务错误码。 */
    public String getCode() {
        return code;
    }
}
