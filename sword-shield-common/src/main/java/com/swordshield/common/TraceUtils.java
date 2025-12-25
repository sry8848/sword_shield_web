package com.swordshield.common;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

public final class TraceUtils {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String OTEL_TRACE_ID_MDC_KEY = "trace_id";
    public static final String TRACEPARENT_HEADER = "traceparent";

    private TraceUtils() {}

    public static String ensureTraceId(String incoming) {
        return StringUtils.hasText(incoming) ? incoming : UUID.randomUUID().toString().replace("-", "");
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
