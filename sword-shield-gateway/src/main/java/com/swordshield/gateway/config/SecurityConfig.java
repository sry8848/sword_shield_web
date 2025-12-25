package com.swordshield.gateway.config;

import com.swordshield.common.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class SecurityConfig {
    private static final String DEFAULT_SECRET = "swordshield-dev-secret-change-me";
    private static final String ADMIN_PREFIX = "admin_";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         GatewaySecurityProperties props,
                                                         Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthConverter) {
        String[] permitAll = props.getPermitAll().toArray(new String[0]);
        String[] adminPaths = props.getAdminPaths().toArray(new String[0]);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonAuthEntryPoint())
                        .accessDeniedHandler(jsonAccessDeniedHandler())
                )
                .authorizeExchange(exchanges -> {
                    // Some endpoints share the same path for both public reads and admin writes.
                    // Enforce admin authorization by HTTP method first, then apply permit-all paths.
                    exchanges.pathMatchers(HttpMethod.POST, "/api/content/material").hasRole("ADMIN");
                    exchanges.pathMatchers(HttpMethod.DELETE, "/api/content/material/**").hasRole("ADMIN");
                    exchanges.pathMatchers(HttpMethod.POST, "/api/content/media").hasRole("ADMIN");
                    exchanges.pathMatchers(HttpMethod.DELETE, "/api/content/media/**").hasRole("ADMIN");

                    if (permitAll.length > 0) {
                        exchanges.pathMatchers(permitAll).permitAll();
                    }
                    if (adminPaths.length > 0) {
                        exchanges.pathMatchers(adminPaths).hasRole("ADMIN");
                    }
                    exchanges.anyExchange().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                )
                .build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(@Value("${jwt.secret:}") String jwtSecretProperty) {
        String secret = resolveSecret(jwtSecretProperty);
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("jwt.secret/JWT_SECRET must be at least 32 bytes for HS256");
        }
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(key).build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }

    @Bean
    public Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(this::authoritiesFromSubject);
        return new ReactiveJwtAuthenticationConverterAdapter(delegate);
    }

    private List<GrantedAuthority> authoritiesFromSubject(Jwt jwt) {
        String subject = jwt == null ? null : jwt.getSubject();
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (StringUtils.hasText(subject) && subject.startsWith(ADMIN_PREFIX)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return authorities;
    }

    private String resolveSecret(String jwtSecretProperty) {
        if (StringUtils.hasText(jwtSecretProperty)) {
            return jwtSecretProperty.trim();
        }
        String env = System.getenv("JWT_SECRET");
        if (StringUtils.hasText(env)) {
            return env.trim();
        }
        String prop = System.getProperty("jwt.secret");
        if (StringUtils.hasText(prop)) {
            return prop.trim();
        }
        return DEFAULT_SECRET;
    }

    private ServerAuthenticationEntryPoint jsonAuthEntryPoint() {
        return (exchange, ex) -> writeJson(exchange, HttpStatus.UNAUTHORIZED, Response.error(401, "未登录，请先登录"));
    }

    private ServerAccessDeniedHandler jsonAccessDeniedHandler() {
        return (exchange, ex) -> writeJson(exchange, HttpStatus.FORBIDDEN, Response.error(403, "无权限访问"));
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, Object body) {
        byte[] bytes;
        try {
            bytes = OBJECT_MAPPER.writeValueAsBytes(body);
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(status);
            return exchange.getResponse().setComplete();
        }
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setContentLength(bytes.length);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }
}
