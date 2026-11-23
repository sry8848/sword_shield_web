package com.swordshield.next.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.handler.ExceptionHandlingWebHandler;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
final class GatewayAccessLog implements WebFilter {

    static final String TARGET_SERVICE = "target-service";
    private static final Logger LOG = LoggerFactory.getLogger(GatewayAccessLog.class);

    /** 记录从请求进入到响应提交的耗时及实际状态。 */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startedAt = System.nanoTime();
        // WebFlux 最外层的异常处理会在过滤链之外写响应，提交回调也能观察到该状态。
        exchange.getResponse().beforeCommit(() -> Mono.fromRunnable(() -> log(exchange, startedAt)));
        return chain.filter(exchange);
    }

    /** 只记录路由配置和异常类型等安全字段，例行健康检查使用 DEBUG。 */
    private static void log(ServerWebExchange exchange, long startedAt) {
        // 1. 读取已匹配的路由和框架处理的异常。
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        Throwable failure = exchange.getAttribute(ExceptionHandlingWebHandler.HANDLED_WEB_EXCEPTION);
        var responseStatus = exchange.getResponse().getStatusCode();
        int status = responseStatus == null ? 200 : responseStatus.value();
        boolean healthy = status < 400 && ("/actuator/health".equals(exchange.getRequest().getPath().value())
                || (route != null && "identity-health".equals(route.getId())));
        var log = healthy ? LOG.atDebug() : failure != null && status >= 500 ? LOG.atWarn() : LOG.atInfo();

        // 2. 关联安全的配置标识和耗时，MDC 从 HTTP 请求追踪上下文附上 trace/span。
        log.addKeyValue("http.request.method", exchange.getRequest().getMethod().name())
                .addKeyValue("http.response.status_code", status)
                .addKeyValue("event.duration", System.nanoTime() - startedAt);
        if (route != null) {
            log.addKeyValue("gateway.route.id", route.getId());
            Object target = route.getMetadata().get(TARGET_SERVICE);
            if (target != null) {
                log.addKeyValue("gateway.target.service", target);
            }
            String pattern = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_PREDICATE_MATCHED_PATH_ATTR);
            if (pattern != null && route.getId().equals(
                    exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR))) {
                log.addKeyValue("http.route", pattern);
            }
        }
        if (failure != null) {
            log.addKeyValue("error.type", NestedExceptionUtils.getMostSpecificCause(failure).getClass().getSimpleName());
        }
        log.log("Gateway 请求摘要");
    }
}
