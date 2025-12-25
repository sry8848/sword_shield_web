package com.swordshield.gateway;

import com.swordshield.common.TraceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-100)
public class TraceGlobalFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(TraceGlobalFilter.class);

    private static final String EXCHANGE_BEFORE_COMMIT_ATTR = "ss.trace.beforeCommit";

    private final GatewayTraceContext traceContext;

    public TraceGlobalFilter(GatewayTraceContext traceContext) {
        this.traceContext = traceContext;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startNanos = System.nanoTime();
        String method = exchange.getRequest().getMethod() == null ? "?" : exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getRawPath();

        registerBeforeCommitOnce(exchange);
        return chain.filter(exchange)
                .doFinally(signalType -> logAccess(exchange, startNanos, method, path));
    }

    private void registerBeforeCommitOnce(ServerWebExchange exchange) {
        if (exchange.getAttribute(EXCHANGE_BEFORE_COMMIT_ATTR) != null) {
            return;
        }
        exchange.getAttributes().put(EXCHANGE_BEFORE_COMMIT_ATTR, Boolean.TRUE);
        exchange.getResponse().beforeCommit(() -> {
            GatewayTraceContext.TraceInfo traceInfo = traceContext.ensureTraceInfo(exchange);
            if (traceInfo != null && StringUtils.hasText(traceInfo.traceId())) {
                exchange.getResponse().getHeaders().set(TraceUtils.TRACE_ID_HEADER, traceInfo.traceId());
            }
            return Mono.empty();
        });
    }

    private void logAccess(ServerWebExchange exchange, long startNanos, String method, String path) {
        GatewayTraceContext.TraceInfo traceInfo = traceContext.ensureTraceInfo(exchange);
        if (traceInfo == null || !StringUtils.hasText(traceInfo.traceId())) {
            return;
        }
        MDC.put(TraceUtils.TRACE_ID_MDC_KEY, traceInfo.traceId());
        MDC.put(TraceUtils.OTEL_TRACE_ID_MDC_KEY, traceInfo.traceId());
        try {
            int status = exchange.getResponse().getStatusCode() == null ? 0 : exchange.getResponse().getStatusCode().value();
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("GW {} {} -> {} ({} ms)", method, path, status, durationMs);
        } finally {
            MDC.remove(TraceUtils.TRACE_ID_MDC_KEY);
            MDC.remove(TraceUtils.OTEL_TRACE_ID_MDC_KEY);
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
