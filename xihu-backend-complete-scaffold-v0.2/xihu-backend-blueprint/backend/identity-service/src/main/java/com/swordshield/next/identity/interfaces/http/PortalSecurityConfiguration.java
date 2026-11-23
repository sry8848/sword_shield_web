package com.swordshield.next.identity.interfaces.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
class PortalSecurityConfiguration {

    /** 为 Portal 当前身份入口建立无状态的资源服务安全链。 */
    @Bean
    SecurityFilterChain portalSecurity(HttpSecurity http, JwtDecoder decoder,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver errors) throws Exception {
        // 1. 登录和 JWKS 是公共入口，本安全链只处理需要 Bearer 的当前身份请求。
        return http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/portal/me"))
                .authorizeHttpRequests(rules -> rules.anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 此链只接受 Authorization Bearer，浏览器 Cookie 不参与认证。
                .csrf(csrf -> csrf.disable())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.decoder(decoder))
                        .authenticationEntryPoint((req, res, ex) -> unauthorized(req, res, ex, errors)))
                .build();
    }

    /** 保留 Bearer 挑战头，再复用现有 MVC Problem Details 处理器。 */
    private static void unauthorized(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failure, HandlerExceptionResolver errors) {
        // 1. 只有验签通过后的过期校验才返回可续期的错误码。
        boolean expired = failure.getCause() instanceof JwtValidationException invalid
                && invalid.getErrors().stream().anyMatch(error -> error.getErrorCode().equals("ACCESS_TOKEN_EXPIRED"));
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        var problem = expired
                ? new PortalAuthProblem(HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN_EXPIRED", "未认证", "访问令牌已过期")
                : new PortalAuthProblem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "未认证", "访问令牌缺失或无效");
        errors.resolveException(request, response, null, problem);
    }
}
