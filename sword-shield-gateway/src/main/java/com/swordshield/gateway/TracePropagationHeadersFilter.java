package com.swordshield.gateway;

import com.swordshield.common.TraceUtils;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * 链路追踪头信息传播过滤器
 * 负责在HTTP请求头中添加链路追踪相关的头信息，用于分布式链路追踪
 */
@Component
public class TracePropagationHeadersFilter implements HttpHeadersFilter, Ordered {
    private final GatewayTraceContext traceContext;

    /**
     * 构造函数
     * @param traceContext 链路追踪上下文对象
     */
    public TracePropagationHeadersFilter(GatewayTraceContext traceContext) {
        this.traceContext = traceContext;
    }

    /**
     * 过滤HTTP头信息，添加链路追踪相关头信息
     * @param input 原始HTTP头信息
     * @param exchange 服务器Web交换对象
     * @return 包含链路追踪头信息的HTTP头信息
     */
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

    /**
     * 判断是否支持指定类型的头信息过滤
     * @param type 头信息过滤类型
     * @return 是否支持
     */
    @Override
    public boolean supports(Type type) {
        return type == Type.REQUEST;
    }

    /**
     * 获取过滤器执行顺序
     * @return 执行顺序值，返回最低优先级确保在其他过滤器之后执行
     */
    @Override
    public int getOrder() {
        // Run late so even if other filters touch headers, trace headers are present on the outbound request.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
