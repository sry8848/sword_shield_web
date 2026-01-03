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

/**
 * 全局链路追踪过滤器
 * 负责在网关层面处理请求的链路追踪，包括生成traceId、记录请求日志、设置响应头等
 */
@Component
@Order(-100)
public class TraceGlobalFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(TraceGlobalFilter.class);

    private static final String EXCHANGE_BEFORE_COMMIT_ATTR = "ss.trace.beforeCommit";

    private final GatewayTraceContext traceContext;

    /**
     * 构造函数
     * @param traceContext 链路追踪上下文
     */
    public TraceGlobalFilter(GatewayTraceContext traceContext) {
        this.traceContext = traceContext;
    }

    /**
     * 过滤器主方法，处理请求的链路追踪逻辑
     * @param exchange 服务器Web交换对象
     * @param chain 过滤器链
     * @return Mono<Void> 异步处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startNanos = System.nanoTime();
        String method = exchange.getRequest().getMethod() == null ? "?" : exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getRawPath();

        // 注册响应提交前的回调，用于设置traceId响应头
        registerBeforeCommitOnce(exchange);
        return chain.filter(exchange)
                .doFinally(signalType -> logAccess(exchange, startNanos, method, path));
    }

    /**
     * 注册响应提交前的回调函数，确保只注册一次
     * 用于在响应提交前将traceId添加到响应头中
     * @param exchange 服务器Web交换对象
     */
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

    /**
     * 记录访问日志
     * 在请求处理完成后记录请求方法、路径、状态码和处理时长
     * @param exchange 服务器Web交换对象
     * @param startNanos 请求开始时间（纳秒）
     * @param method HTTP请求方法
     * @param path 请求路径
     */
    private void logAccess(ServerWebExchange exchange, long startNanos, String method, String path) {
        GatewayTraceContext.TraceInfo traceInfo = traceContext.ensureTraceInfo(exchange);
        if (traceInfo == null || !StringUtils.hasText(traceInfo.traceId())) {
            return;
        }
        // 设置MDC上下文，用于日志输出中的traceId
        MDC.put(TraceUtils.TRACE_ID_MDC_KEY, traceInfo.traceId());
        MDC.put(TraceUtils.OTEL_TRACE_ID_MDC_KEY, traceInfo.traceId());
        try {
            int status = exchange.getResponse().getStatusCode() == null ? 0 : exchange.getResponse().getStatusCode().value();
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("GW {} {} -> {} ({} ms)", method, path, status, durationMs);
        } finally {
            // 清理MDC上下文，避免影响后续请求
            MDC.remove(TraceUtils.TRACE_ID_MDC_KEY);
            MDC.remove(TraceUtils.OTEL_TRACE_ID_MDC_KEY);
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
