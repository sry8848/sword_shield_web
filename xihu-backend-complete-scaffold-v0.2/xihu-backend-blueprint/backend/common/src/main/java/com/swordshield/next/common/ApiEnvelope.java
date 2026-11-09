package com.swordshield.next.common;

import java.time.Instant;

public record ApiEnvelope<T>(String code, String message, T data, String traceId, Instant timestamp) {

    /**
     * 将业务数据和接口层提供的请求 Trace ID 包装为成功响应。
     *
     * @param data 业务数据
     * @param traceId 由接口层从当前请求追踪上下文取得的标识
     * @return 含追踪信息的成功响应
     * @throws IllegalArgumentException Trace ID 为空时抛出
     */
    public static <T> ApiEnvelope<T> ok(T data, String traceId) {
        // 1. 拒绝缺少 Trace ID 的调用。
        if (traceId == null || traceId.isBlank()) {
            // 缺少 Trace ID 会使响应无法关联请求日志，直接暴露接口层的调用错误。
            throw new IllegalArgumentException("traceId must not be blank");
        }

        // 2. 返回带追踪信息及生成时间的成功响应。
        return new ApiEnvelope<>("SUCCESS", "操作成功", data, traceId, Instant.now());
    }
}
