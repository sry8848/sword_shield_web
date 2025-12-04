package com.swordshield.common;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

public final class TraceUtils {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private TraceUtils() {}

    public static String ensureTraceId(String incoming) {
        return StringUtils.hasText(incoming) ? incoming : UUID.randomUUID().toString().replace("-", "");
    }

    public static Optional<String> currentTraceId() {
        return Optional.ofNullable(MDC.get(TRACE_ID_MDC_KEY));
    }
}
