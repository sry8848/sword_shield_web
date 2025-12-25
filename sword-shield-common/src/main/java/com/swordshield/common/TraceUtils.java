package com.swordshield.common;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

public final class TraceUtils {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String OTEL_TRACE_ID_MDC_KEY = "trace_id";
    public static final String TRACEPARENT_HEADER = "traceparent";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private TraceUtils() {}

    public static String ensureTraceId(String incoming) {
        return StringUtils.hasText(incoming) ? incoming : UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Ensures a W3C-compatible traceId (32 lowercase hex chars).
     * If the incoming value is missing or invalid, a new one is generated.
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

    public static boolean isValidTraceId(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return false;
        }
        String trimmed = traceId.trim();
        return isValidHex(trimmed, 32) && !isAllZerosHex(trimmed);
    }

    public static boolean isValidSpanId(String spanId) {
        if (!StringUtils.hasText(spanId)) {
            return false;
        }
        String trimmed = spanId.trim();
        return isValidHex(trimmed, 16) && !isAllZerosHex(trimmed);
    }

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

    public static Optional<String> traceIdFromTraceParent(String traceParent) {
        if (!isValidTraceParent(traceParent)) {
            return Optional.empty();
        }
        // traceparent = "00-<traceId>-<spanId>-<flags>"
        String traceId = traceParent.trim().split("-")[1];
        return Optional.of(traceId.toLowerCase());
    }

    public static String newTraceParent(String traceId) {
        String tid = ensureW3cTraceId(traceId);
        return "00-" + tid + "-" + newSpanId() + "-01";
    }

    public static String newSpanId() {
        byte[] bytes = new byte[8];
        do {
            RANDOM.nextBytes(bytes);
        } while (isAllZeros(bytes));
        return HEX.formatHex(bytes);
    }

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

    private static boolean isAllZerosHex(String hex) {
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if (c != '0') {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllZeros(byte[] bytes) {
        for (byte b : bytes) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    public static Optional<String> currentOtelTraceId() {
        SpanContext spanContext = Span.current().getSpanContext();
        if (spanContext.isValid()) {
            return Optional.of(spanContext.getTraceId());
        }
        return Optional.empty();
    }

    public static Optional<String> currentTraceParent() {
        SpanContext spanContext = Span.current().getSpanContext();
        if (!spanContext.isValid()) {
            return Optional.empty();
        }
        String traceParent = "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + spanContext.getTraceFlags().asHex();
        return Optional.of(traceParent);
    }

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
