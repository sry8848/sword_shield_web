package com.swordshield.gateway;

import com.swordshield.common.TraceUtils;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * 网关追踪上下文管理器，负责处理分布式追踪信息的获取、缓存和传播
 */
@Component
public class GatewayTraceContext {
    private static final String EXCHANGE_TRACE_ID_ATTR = "ss.traceId";
    private static final String EXCHANGE_TRACEPARENT_ATTR = "ss.traceparent";

    private final Tracer tracer;

    /**
     * 构造函数
     * @param tracer 追踪器实例，用于获取当前活动的追踪上下文
     */
    public GatewayTraceContext(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * 确保交换对象中存在追踪信息，按优先级获取：活动span > 缓存 > 请求头 > 新生成
     * @param exchange 服务器Web交换对象，包含请求和响应信息
     * @return 包含追踪ID和traceparent的追踪信息对象
     */
    public TraceInfo ensureTraceInfo(ServerWebExchange exchange) {
        // Prefer the active Micrometer trace/span if present; it is the source of truth for propagation and Jaeger.
        TraceInfo fromSpan = currentTraceInfoFromSpan();
        if (fromSpan != null) {
            // Overwrite any cached values computed before the span was established.
            exchange.getAttributes().put(EXCHANGE_TRACE_ID_ATTR, fromSpan.traceId());
            exchange.getAttributes().put(EXCHANGE_TRACEPARENT_ATTR, fromSpan.traceParent());
            return fromSpan;
        }

        String cachedTraceId = exchange.getAttribute(EXCHANGE_TRACE_ID_ATTR);
        String cachedTraceParent = exchange.getAttribute(EXCHANGE_TRACEPARENT_ATTR);
        if (StringUtils.hasText(cachedTraceId) && StringUtils.hasText(cachedTraceParent)) {
            return new TraceInfo(cachedTraceId, cachedTraceParent);
        }

        String headerTraceParent = exchange.getRequest().getHeaders().getFirst(TraceUtils.TRACEPARENT_HEADER);
        String traceId = TraceUtils.traceIdFromTraceParent(headerTraceParent)
                .orElseGet(() -> TraceUtils.ensureW3cTraceId(exchange.getRequest().getHeaders().getFirst(TraceUtils.TRACE_ID_HEADER)));
        String traceParent = TraceUtils.isValidTraceParent(headerTraceParent) ? headerTraceParent.trim() : TraceUtils.newTraceParent(traceId);

        TraceInfo computed = new TraceInfo(traceId, traceParent);
        cacheTraceInfo(exchange, computed);
        return computed;
    }

    /**
     * 将追踪信息缓存到交换对象的属性中
     * @param exchange 服务器Web交换对象
     * @param traceInfo 追踪信息对象
     */
    private void cacheTraceInfo(ServerWebExchange exchange, TraceInfo traceInfo) {
        if (traceInfo == null || !StringUtils.hasText(traceInfo.traceId()) || !StringUtils.hasText(traceInfo.traceParent())) {
            return;
        }
        exchange.getAttributes().putIfAbsent(EXCHANGE_TRACE_ID_ATTR, traceInfo.traceId());
        exchange.getAttributes().putIfAbsent(EXCHANGE_TRACEPARENT_ATTR, traceInfo.traceParent());
    }

    /**
     * 从当前活动的span中获取追踪信息
     * @return 追踪信息对象，如果当前没有活动span则返回null
     */
    private TraceInfo currentTraceInfoFromSpan() {
        Span span = tracer.currentSpan();
        if (span == null) {
            return null;
        }
        TraceContext context = span.context();
        if (context == null || !StringUtils.hasText(context.traceId()) || !StringUtils.hasText(context.spanId())) {
            return null;
        }
        String traceId = context.traceId().toLowerCase();
        String spanId = context.spanId().toLowerCase();
        String sampledFlag = Boolean.FALSE.equals(context.sampled()) ? "00" : "01";
        String traceParent = "00-" + traceId + "-" + spanId + "-" + sampledFlag;
        return new TraceInfo(traceId, traceParent);
    }

    /**
     * 追踪信息记录类，包含追踪ID和traceparent字符串
     * @param traceId 追踪ID
     * @param traceParent W3C traceparent字符串
     */
    public record TraceInfo(String traceId, String traceParent) {}
}
