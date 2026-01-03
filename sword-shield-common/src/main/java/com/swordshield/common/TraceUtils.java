package com.swordshield.common;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * 追踪工具类，提供追踪ID的生成、验证和管理功能
 * 支持W3C Trace Context标准和OpenTelemetry追踪
 */
public final class TraceUtils {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String OTEL_TRACE_ID_MDC_KEY = "trace_id";
    public static final String TRACEPARENT_HEADER = "traceparent";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private TraceUtils() {}

    /**
     * 确保返回一个有效的追踪ID
     * 如果传入的追踪ID有效则返回原值，否则生成新的UUID作为追踪ID
     *
     * @param incoming 输入的追踪ID，可能为null或空
     * @return 有效的追踪ID字符串
     */
    public static String ensureTraceId(String incoming) {
        return StringUtils.hasText(incoming) ? incoming : UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 确保返回一个W3C兼容的追踪ID（32位小写十六进制字符）
     * 如果传入的值缺失或无效，则生成一个新的
     *
     * @param incoming 输入的追踪ID，可能为null或空
     * @return W3C兼容的追踪ID字符串
     */
    public static String ensureW3cTraceId(String incoming) {
        if (StringUtils.hasText(incoming)) {
            String trimmed = incoming.trim();
            if (isValidTraceId(trimmed)) {
                return trimmed.toLowerCase();
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 验证追踪ID是否有效
     * 追踪ID必须是32位十六进制字符且不能全为0
     *
     * @param traceId 待验证的追踪ID
     * @return 如果追踪ID有效返回true，否则返回false
     */
    public static boolean isValidTraceId(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return false;
        }
        String trimmed = traceId.trim();
        return isValidHex(trimmed, 32) && !isAllZerosHex(trimmed);
    }

    /**
     * 验证跨度ID是否有效
     * 跨度ID必须是16位十六进制字符且不能全为0
     *
     * @param spanId 待验证的跨度ID
     * @return 如果跨度ID有效返回true，否则返回false
     */
    public static boolean isValidSpanId(String spanId) {
        if (!StringUtils.hasText(spanId)) {
            return false;
        }
        String trimmed = spanId.trim();
        return isValidHex(trimmed, 16) && !isAllZerosHex(trimmed);
    }

    /**
     * 验证traceparent字符串是否有效
     * traceparent格式为"version-traceId-spanId-flags"
     *
     * @param traceParent 待验证的traceparent字符串
     * @return 如果traceparent有效返回true，否则返回false
     */
    public static boolean isValidTraceParent(String traceParent) {
        if (!StringUtils.hasText(traceParent)) {
            return false;
        }
        String[] parts = traceParent.trim().split("-");
        if (parts.length != 4) {
            return false;
        }
        String version = parts[0];
        if (!isValidHex(version, 2) || "ff".equalsIgnoreCase(version)) {
            return false;
        }
        return isValidTraceId(parts[1]) && isValidSpanId(parts[2]) && isValidHex(parts[3], 2);
    }

    /**
     * 从traceparent字符串中提取追踪ID
     *
     * @param traceParent traceparent字符串
     * @return 包含追踪ID的Optional对象，如果traceparent无效则返回空Optional
     */
    public static Optional<String> traceIdFromTraceParent(String traceParent) {
        if (!isValidTraceParent(traceParent)) {
            return Optional.empty();
        }
        // traceparent = "00-<traceId>-<spanId>-<flags>"
        String traceId = traceParent.trim().split("-")[1];
        return Optional.of(traceId.toLowerCase());
    }

    /**
     * 根据给定的追踪ID创建新的traceparent字符串
     *
     * @param traceId 基础追踪ID
     * @return 新的traceparent字符串，格式为"00-<traceId>-<spanId>-01"
     */
    public static String newTraceParent(String traceId) {
        String tid = ensureW3cTraceId(traceId);
        return "00-" + tid + "-" + newSpanId() + "-01";
    }

    /**
     * 生成新的跨度ID
     * 生成的ID为16位十六进制字符串，且不能全为0
     *
     * @return 新的跨度ID字符串
     */
    public static String newSpanId() {
        byte[] bytes = new byte[8];
        do {
            RANDOM.nextBytes(bytes);
        } while (isAllZeros(bytes));
        return HEX.formatHex(bytes);
    }

    /**
     * 验证字符串是否为指定长度的有效十六进制字符串
     *
     * @param value 待验证的字符串
     * @param expectedLength 期望的长度
     * @return 如果是有效十六进制字符串返回true，否则返回false
     */
    private static boolean isValidHex(String value, int expectedLength) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        if (value.length() != expectedLength) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查十六进制字符串是否全为零
     *
     * @param hex 十六进制字符串
     * @return 如果全为零返回true，否则返回false
     */
    private static boolean isAllZerosHex(String hex) {
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if (c != '0') {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查字节数组是否全为零
     *
     * @param bytes 字节数组
     * @return 如果全为零返回true，否则返回false
     */
    private static boolean isAllZeros(byte[] bytes) {
        for (byte b : bytes) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取当前OpenTelemetry追踪上下文中的追踪ID
     *
     * @return 包含当前追踪ID的Optional对象，如果没有有效追踪上下文则返回空Optional
     */
    public static Optional<String> currentOtelTraceId() {
        SpanContext spanContext = Span.current().getSpanContext();
        if (spanContext.isValid()) {
            return Optional.of(spanContext.getTraceId());
        }
        return Optional.empty();
    }

    /**
     * 获取当前OpenTelemetry追踪上下文的traceparent字符串
     *
     * @return 包含traceparent字符串的Optional对象，如果没有有效追踪上下文则返回空Optional
     */
    public static Optional<String> currentTraceParent() {
        SpanContext spanContext = Span.current().getSpanContext();
        if (!spanContext.isValid()) {
            return Optional.empty();
        }
        String traceParent = "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + spanContext.getTraceFlags().asHex();
        return Optional.of(traceParent);
    }

    /**
     * 获取当前追踪ID，优先级：OpenTelemetry -> MDC中的traceId -> MDC中的trace_id
     *
     * @return 包含当前追踪ID的Optional对象，如果都不存在则返回空Optional
     */
    public static Optional<String> currentTraceId() {
        Optional<String> otel = currentOtelTraceId();
        if (otel.isPresent()) {
            return otel;
        }
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        if (StringUtils.hasText(traceId)) {
            return Optional.of(traceId);
        }
        String otelTraceIdFromMdc = MDC.get(OTEL_TRACE_ID_MDC_KEY);
        return StringUtils.hasText(otelTraceIdFromMdc) ? Optional.of(otelTraceIdFromMdc) : Optional.empty();
    }
}
