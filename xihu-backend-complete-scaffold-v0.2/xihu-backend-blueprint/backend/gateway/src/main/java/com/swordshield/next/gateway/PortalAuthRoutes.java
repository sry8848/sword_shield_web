package com.swordshield.next.gateway;

import java.net.InetAddress;

import io.netty.util.NetUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;

@Configuration(proxyBeanMethods = false)
class PortalAuthRoutes {

    /** 将已实现的 Portal 认证入口转发到 Identity，登录路由在网关边界确认来源地址。 */
    @Bean
    RouteLocator portalAuth(RouteLocatorBuilder routes,
            @Value("${gateway.identity-uri:http://127.0.0.1:8081}") String identityUri,
            @Value("${gateway.portal.trusted-proxy-ip:}") String trustedProxyIp) {
        return routes.routes()
                .route("portal-login", route -> route.path("/api/v1/auth/portal/login")
                        .filters(filters -> filters.filter((exchange, chain) -> {
                            // 外部请求提供的同名地址头始终由网关重新设置。
                            var request = exchange.getRequest().mutate()
                                    .headers(headers -> headers.set("X-Identity-Client-IP", clientIp(exchange, trustedProxyIp)))
                                    .build();
                            return chain.filter(exchange.mutate().request(request).build());
                        }))
                        .metadata(GatewayAccessLog.TARGET_SERVICE, "identity-service")
                        .uri(identityUri))
                .route("portal-identity", route -> route.path("/api/v1/auth/portal/me", "/api/v1/auth/jwks")
                        .metadata(GatewayAccessLog.TARGET_SERVICE, "identity-service")
                        .uri(identityUri))
                .build();
    }

    /** 取直接对端地址，或取明确配置的反向代理加在转发链尾部的地址。 */
    private static String clientIp(ServerWebExchange exchange, String trustedProxyIp) {
        // 1. 只有眼前的网络对端是受信代理，才读取其写入的地址。
        String peer = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        var forwarded = exchange.getRequest().getHeaders().getOrEmpty("X-Forwarded-For");
        if (peer.equals(trustedProxyIp) && !forwarded.isEmpty()) {
            String lastHeader = forwarded.get(forwarded.size() - 1);
            InetAddress client = NetUtil.createInetAddressFromIpAddressString(
                    lastHeader.substring(lastHeader.lastIndexOf(',') + 1).trim());
            if (client != null) {
                return client.getHostAddress();
            }
        }
        return peer;
    }
}
