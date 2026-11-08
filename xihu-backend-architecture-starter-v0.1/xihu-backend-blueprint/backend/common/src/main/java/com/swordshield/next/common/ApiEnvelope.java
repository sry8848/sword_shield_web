package com.swordshield.next.common;
import java.time.Instant;
import java.util.UUID;
public record ApiEnvelope<T>(String code, String message, T data, String traceId, Instant timestamp) {
    public static <T> ApiEnvelope<T> ok(T data) {
        String trace = org.slf4j.MDC.get("traceId");
        return new ApiEnvelope<>("SUCCESS", "操作成功", data, trace == null ? UUID.randomUUID().toString() : trace, Instant.now());
    }
}
