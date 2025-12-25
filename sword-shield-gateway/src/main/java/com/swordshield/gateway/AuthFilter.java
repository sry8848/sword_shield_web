package com.swordshield.gateway;

import com.swordshield.common.AuthUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AuthFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String path = req.getURI().getPath();
        log.info("[AuthFilter] Request path: {}", path);
        if (
                path.startsWith("/swagger-ui") ||
                path.startsWith("/webjars/swagger-ui") ||
                path.contains("/v3/api-docs") ||
                path.startsWith("/api/user/login") ||
                path.startsWith("/api/user/register") ||
                path.startsWith("/api/admin/auth/login") ||
                path.startsWith("/api/activity/list") ||
                path.startsWith("/api/activity/detail") ||
                path.startsWith("/api/content/articles") ||
                path.startsWith("/api/content/article") ||
                path.startsWith("/api/content/exhibitors") ||
                path.startsWith("/api/content/exhibitor") ||
                path.startsWith("/api/content/achievements") ||
                path.startsWith("/api/content/achievement") ||
                path.startsWith("/api/content/poster")
        ) {
            return chain.filter(exchange);
        }
        if (path.startsWith("/api/file/presigned")) {
            return chain.filter(exchange);
        }
        if (path.startsWith("/api/interaction/comments")) {
            return chain.filter(exchange);
        }
        String auth = req.getHeaders().getFirst("Authorization");
        if (AuthUtils.isValidBearerToken(auth)) {
            return chain.filter(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() { return -100; }
}

