package com.swordshield.next.identity.interfaces.http;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
class AdminSecurityConfiguration {

    @Bean
    HttpSessionCsrfTokenRepository adminCsrfTokenRepository() {
        var repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }

    /** 建立独立的 Admin 有状态安全链，处理浏览器来源、按会话状态触发的 CSRF 及 Problem Details。 */
    @Bean
    SecurityFilterChain adminSecurity(HttpSecurity http,
            HttpSessionCsrfTokenRepository csrfTokenRepository,
            DefaultCookieSerializer cookieSerializer,
            @Value("${identity.admin.origin:http://localhost:5174}") String allowedOrigin,
            @Value("${identity.admin.trusted-gateway-ip:127.0.0.1}") String trustedGatewayIp,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver errors) throws Exception {
        var matcher = PathPatternRequestMatcher.withDefaults();
        return http.securityMatchers(matchers -> matchers
                        .requestMatchers(matcher.matcher("/api/v1/auth/admin/**"),
                                matcher.matcher("/internal/admin/session/**")))
                .authorizeHttpRequests(rules -> rules.anyRequest().permitAll())
                .requestCache(cache -> cache.disable())
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                            FilterChain filterChain) throws ServletException, IOException {
                        // 内部校验仅从网关所在连接接收，不接受客户端代填的来源头。
                        if (request.getRequestURI().startsWith("/internal/")
                                && !trustedGatewayIp.equals(request.getRemoteAddr())) {
                            errors.resolveException(request, response, null,
                                    new PortalAuthProblem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "未找到", "目标资源不存在。"));
                            return;
                        }
                        if (CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request)
                                && !allowedOrigin.equals(request.getHeader(HttpHeaders.ORIGIN))) {
                            errors.resolveException(request, response, null,
                                    new PortalAuthProblem(HttpStatus.FORBIDDEN, "FORBIDDEN", "禁止访问", "请求来源未获授权"));
                            return;
                        }
                        filterChain.doFilter(request, response);
                    }
                }, CsrfFilter.class)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(request -> requiresCsrf(request, cookieSerializer)))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((req, res, ex) -> errors.resolveException(req, res, null,
                                new PortalAuthProblem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "未认证", "后台 Session 缺失或无效")))
                        .accessDeniedHandler((req, res, ex) -> {
                            String detail = ex instanceof CsrfException ? "CSRF 令牌缺失或无效" : "无权访问目标资源";
                            errors.resolveException(req, res, null,
                                    new PortalAuthProblem(HttpStatus.FORBIDDEN, "FORBIDDEN", "禁止访问", detail));
                        }))
                .build();
    }

    /**
     * 登录写请求始终要求有效 CSRF；退出与内部 ForwardAuth 写校验仅在命中已认证 Session 时要求 CSRF，
     * 使缺失或已过期的 Session 在退出时幂等返回 200、在业务写校验时返回 401 UNAUTHENTICATED。
     */
    private static boolean requiresCsrf(HttpServletRequest request, DefaultCookieSerializer cookieSerializer) {
        if (!CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request)) {
            return false;
        }
        String uri = request.getRequestURI();
        if ("/api/v1/auth/admin/login".equals(uri)) {
            return true;
        }
        if (cookieSerializer.readCookieValues(request).isEmpty()) {
            return false;
        }
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute(AdminSessionConfiguration.ACCOUNT_ID_ATTR) != null;
    }
}
