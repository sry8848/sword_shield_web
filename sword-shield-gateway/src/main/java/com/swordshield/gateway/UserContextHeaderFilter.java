package com.swordshield.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UserContextHeaderFilter implements GlobalFilter, Ordered {
    public static final String HEADER_AUTH_SUBJECT = "X-Auth-Subject";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_ADMIN_ID = "X-Admin-Id";
    public static final String HEADER_AUTH_VERIFIED = "X-Auth-Verified";

    private static final String ADMIN_PREFIX = "admin_";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .flatMap(auth -> {
                    if (auth instanceof JwtAuthenticationToken jwtAuth) {
                        String subject = jwtAuth.getToken().getSubject();
                        return chain.filter(withUserHeaders(exchange, subject));
                    }
                    return chain.filter(clearUserHeaders(exchange));
                })
                .switchIfEmpty(chain.filter(clearUserHeaders(exchange)));
    }

    private ServerWebExchange withUserHeaders(ServerWebExchange exchange, String subject) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        builder.headers(h -> {
            h.remove(HEADER_AUTH_SUBJECT);
            h.remove(HEADER_USER_ID);
            h.remove(HEADER_ADMIN_ID);
            h.remove(HEADER_AUTH_VERIFIED);
        });

        if (StringUtils.hasText(subject)) {
            builder.header(HEADER_AUTH_SUBJECT, subject);
            builder.header(HEADER_AUTH_VERIFIED, "gateway");

            String userId = subject;
            if (subject.startsWith(ADMIN_PREFIX)) {
                String adminId = subject.substring(ADMIN_PREFIX.length());
                if (StringUtils.hasText(adminId)) {
                    builder.header(HEADER_ADMIN_ID, adminId);
                    userId = adminId;
                }
            }
            if (StringUtils.hasText(userId)) {
                builder.header(HEADER_USER_ID, userId);
            }
        }

        return exchange.mutate().request(builder.build()).build();
    }

    private ServerWebExchange clearUserHeaders(ServerWebExchange exchange) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        builder.headers(h -> {
            h.remove(HEADER_AUTH_SUBJECT);
            h.remove(HEADER_USER_ID);
            h.remove(HEADER_ADMIN_ID);
            h.remove(HEADER_AUTH_VERIFIED);
        });
        return exchange.mutate().request(builder.build()).build();
    }

    @Override
    public int getOrder() {
        return -50;
    }
}

