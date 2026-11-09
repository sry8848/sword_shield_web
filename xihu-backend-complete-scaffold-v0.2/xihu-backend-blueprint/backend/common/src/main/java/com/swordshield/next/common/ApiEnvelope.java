package com.swordshield.next.common;

import java.time.Instant;
import org.slf4j.MDC;

public record ApiEnvelope<T>(String code, String message, T data, String traceId, Instant timestamp) {

    /**
     * 将业务数据包装为成功响应，使用当前请求的 Trace ID。
     *
     * @param data 业务数据
     * @return 含追踪信息的成功响应
     * @throws IllegalStateException 当前请求缺少追踪上下文时抛出
     */
    public static <T> ApiEnvelope<T> ok(T data) {
        // 1. 读取由请求追踪框架写入 MDC 的 Trace ID。
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) {
            // 缺少追踪上下文意味着响应无法与请求日志关联，应直接暴露配置问题。
            throw new IllegalStateException("当前请求追踪上下文缺少 traceId");
        }

        // 2. 返回带追踪信息及生成时间的成功响应。
        return new ApiEnvelope<>("SUCCESS", "操作成功", data, traceId, Instant.now());
    }
}
