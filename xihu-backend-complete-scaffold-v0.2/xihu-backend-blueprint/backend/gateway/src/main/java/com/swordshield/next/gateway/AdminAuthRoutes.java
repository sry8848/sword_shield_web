package com.swordshield.next.gateway;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.BooleanSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "gateway.admin.enabled", havingValue = "true")
class AdminAuthRoutes {

    /** 后台认证直通 Identity，后台业务请求在转发前同步校验当前会话。 */
    @Bean
    RouteLocator adminAuth(RouteLocatorBuilder routes, ObservationRegistry observations, Environment environment,
            @Value("${gateway.identity-uri:http://127.0.0.1:8081}") String identityUri,
            @Value("${gateway.activity-uri:http://127.0.0.1:8082}") String activityUri,
            @Value("${gateway.admin.host:}") String adminHost,
            @Value("${gateway.portal.host:}") String portalHost,
            @Value("${gateway.admin.trusted-proxy-ip:}") String trustedProxyIp) {
        // 生产环境启用后台入口前须配置分离的会话主机。
        if (environment.acceptsProfiles(Profiles.of("prod"))
                && (adminHost.isBlank() || portalHost.isBlank() || adminHost.equalsIgnoreCase(portalHost))) {
            throw new IllegalStateException("启用生产后台路由需要分别配置前台和后台主机");
        }

        var forwardAuth = new AdminForwardAuth(observations, identityUri);
        return routes.routes()
                .route("admin-auth", route -> onHost(route.path("/api/v1/auth/admin/**"), adminHost)
                        .filters(filters -> filters.filter((exchange, chain) -> {
                            var request = exchange.getRequest().mutate().headers(headers -> {
                                AuthCookies.remove(headers, AuthCookies.PORTAL);
                                AdminForwardAuth.removeIdentity(headers);
                                headers.set("X-Identity-Client-IP", PortalAuthRoutes.clientIp(exchange, trustedProxyIp));
                            }).build();
                            return chain.filter(exchange.mutate().request(request).build());
                        }))
                        .metadata(GatewayAccessLog.TARGET_SERVICE, "identity-service")
                        .uri(identityUri))
                .route("admin-identity", route -> onHost(route.path(
                                "/api/v1/admin/users", "/api/v1/admin/users/{userId}",
                                "/api/v1/admin/users/{userId}/roles", "/api/v1/admin/roles",
                                "/api/v1/admin/permissions"), adminHost)
                        .filters(filters -> filters.filter(forwardAuth))
                        .metadata(GatewayAccessLog.TARGET_SERVICE, "identity-service")
                        .uri(identityUri))
                .route("admin-activity", route -> onHost(route.order(1).path("/api/v1/admin/**"), adminHost)
                        .filters(filters -> filters.filter(forwardAuth))
                        .metadata(GatewayAccessLog.TARGET_SERVICE, "activity-service")
                        .uri(activityUri))
                .build();
    }

    /** 本地开发可共享 localhost，配置应用主机后同时按路径和主机匹配。 */
    static BooleanSpec onHost(BooleanSpec route, String host) {
        return host.isBlank() ? route : route.and().host(host);
    }
}
