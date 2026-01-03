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

/**
 * 全局过滤器，用于处理用户上下文相关的请求头信息
 * 该过滤器会根据JWT认证信息，在请求头中添加相应的用户标识信息
 */
@Component
public class UserContextHeaderFilter implements GlobalFilter, Ordered {
    /**
     * 认证主体请求头名称
     */
    public static final String HEADER_AUTH_SUBJECT = "X-Auth-Subject";

    /**
     * 用户ID请求头名称
     */
    public static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 管理员ID请求头名称
     */
    public static final String HEADER_ADMIN_ID = "X-Admin-Id";

    /**
     * 认证验证状态请求头名称
     */
    public static final String HEADER_AUTH_VERIFIED = "X-Auth-Verified";

    /**
     * 管理员标识前缀
     */
    private static final String ADMIN_PREFIX = "admin_";

    /**
     * 过滤器主方法，处理请求并添加用户上下文相关的请求头
     *
     * @param exchange 当前的服务器Web交换对象
     * @param chain 过滤器链对象
     * @return Mono<Void> 异步处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .flatMap(auth -> {
                    // 检查是否为JWT认证令牌
                    if (auth instanceof JwtAuthenticationToken jwtAuth) {
                        String subject = jwtAuth.getToken().getSubject();
                        return chain.filter(withUserHeaders(exchange, subject));
                    }
                    return chain.filter(clearUserHeaders(exchange));
                })
                .switchIfEmpty(chain.filter(clearUserHeaders(exchange)));
    }

    /**
     * 为请求添加用户相关的请求头信息
     *
     * @param exchange 原始的服务器Web交换对象
     * @param subject JWT令牌中的主体信息
     * @return ServerWebExchange 包含用户头信息的新交换对象
     */
    private ServerWebExchange withUserHeaders(ServerWebExchange exchange, String subject) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        // 清除现有的用户相关请求头
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
            // 检查是否为管理员用户
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

    /**
     * 清除请求中的用户相关请求头信息
     *
     * @param exchange 原始的服务器Web交换对象
     * @return ServerWebExchange 清除用户头信息后的交换对象
     */
    private ServerWebExchange clearUserHeaders(ServerWebExchange exchange) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        // 清除现有的用户相关请求头
        builder.headers(h -> {
            h.remove(HEADER_AUTH_SUBJECT);
            h.remove(HEADER_USER_ID);
            h.remove(HEADER_ADMIN_ID);
            h.remove(HEADER_AUTH_VERIFIED);
        });
        return exchange.mutate().request(builder.build()).build();
    }

    /**
     * 获取过滤器的执行顺序
     * 数值越小优先级越高
     *
     * @return int 过滤器执行顺序
     */
    @Override
    public int getOrder() {
        return -50;
    }
}

