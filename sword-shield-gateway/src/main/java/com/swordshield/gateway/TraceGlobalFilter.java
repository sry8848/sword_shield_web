package com.swordshield.gateway;

import com.swordshield.common.TraceUtils;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = TraceUtils.ensureTraceId(exchange.getRequest().getHeaders().getFirst(TraceUtils.TRACE_ID_HEADER));

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> headers.set(TraceUtils.TRACE_ID_HEADER, traceId))
                .build();
        exchange.getResponse().getHeaders().set(TraceUtils.TRACE_ID_HEADER, traceId);

        MDC.put(TraceUtils.TRACE_ID_MDC_KEY, traceId);
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .contextWrite(ctx -> ctx.put(TraceUtils.TRACE_ID_MDC_KEY, traceId))
                .doOnEach(signal -> setTraceIdInMdc(signal.getContextView()))
                .doFinally(signalType -> MDC.remove(TraceUtils.TRACE_ID_MDC_KEY));
    }

    private void setTraceIdInMdc(ContextView contextView) {
        if (contextView.hasKey(TraceUtils.TRACE_ID_MDC_KEY)) {
            Object id = contextView.get(TraceUtils.TRACE_ID_MDC_KEY);
            if (id != null) {
                MDC.put(TraceUtils.TRACE_ID_MDC_KEY, id.toString());
            }
        }
    }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
}
