package com.swordshield.gateway;

import com.swordshield.common.TraceUtils;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

@Component
public class TracePropagationHeadersFilter implements HttpHeadersFilter, Ordered {
    private final GatewayTraceContext traceContext;

    public TracePropagationHeadersFilter(GatewayTraceContext traceContext) {
        this.traceContext = traceContext;
    }

    @Override
    public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
        GatewayTraceContext.TraceInfo traceInfo = traceContext.ensureTraceInfo(exchange);
        if (traceInfo == null || !StringUtils.hasText(traceInfo.traceId())) {
            return input;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(input);
        headers.set(TraceUtils.TRACE_ID_HEADER, traceInfo.traceId());
        if (StringUtils.hasText(traceInfo.traceParent())) {
            headers.set(TraceUtils.TRACEPARENT_HEADER, traceInfo.traceParent());
        }
        return headers;
    }

    @Override
    public boolean supports(Type type) {
        return type == Type.REQUEST;
    }

    @Override
    public int getOrder() {
        // Run late so even if other filters touch headers, trace headers are present on the outbound request.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
