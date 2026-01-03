package com.swordshield.gateway.config;

import com.swordshield.common.Response;
import com.swordshield.common.ErrorCode;
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

/**
 * 网关安全配置类
 * 配置Spring Security WebFlux安全策略，包括JWT认证、路径权限控制等
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class SecurityConfig {
    private static final String DEFAULT_SECRET = "swordshield-dev-secret-change-me";
    private static final String ADMIN_PREFIX = "admin_";
    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 配置安全过滤器链
     * 设置CSRF、HTTP基本认证、表单登录、登出等安全策略，配置路径权限和JWT认证
     *
     * @param http 安全配置对象
     * @param props 网关安全属性配置
     * @param jwtAuthConverter JWT认证转换器
     * @return 安全过滤器链
     */
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
                    // 配置特定路径的管理员权限要求
                    // 某些端点对公共读取和管理员写入使用相同路径，首先按HTTP方法强制管理员授权
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

    /**
     * 创建JWT解码器
     * 根据配置的密钥创建NimbusReactiveJwtDecoder，用于解码和验证JWT令牌
     *
     * @param jwtSecretProperty JWT密钥配置属性
     * @return JWT解码器
     */
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

    /**
     * 创建JWT认证转换器
     * 将JWT转换为认证令牌，设置权限转换逻辑
     *
     * @return JWT认证转换器
     */
    @Bean
    public Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(this::authoritiesFromSubject);
        return new ReactiveJwtAuthenticationConverterAdapter(delegate);
    }

    /**
     * 从JWT主题中提取权限信息
     * 根据主题是否以管理员前缀开头来分配管理员权限
     *
     * @param jwt JWT令牌
     * @return 权限列表
     */
    private List<GrantedAuthority> authoritiesFromSubject(Jwt jwt) {
        String subject = jwt == null ? null : jwt.getSubject();
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (StringUtils.hasText(subject) && subject.startsWith(ADMIN_PREFIX)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return authorities;
    }

    /**
     * 解析JWT密钥
     * 按优先级从配置属性、环境变量、系统属性中获取JWT密钥，如果都未配置则使用默认密钥
     *
     * @param jwtSecretProperty JWT密钥配置属性
     * @return 解析后的密钥
     */
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

    /**
     * 创建JSON认证入口点
     * 当用户未认证时返回JSON格式的错误响应
     *
     * @return 认证入口点处理器
     */
    private ServerAuthenticationEntryPoint jsonAuthEntryPoint() {
        return (exchange, ex) -> writeJson(exchange, HttpStatus.UNAUTHORIZED, Response.error(ErrorCode.UNAUTHORIZED, "未登录，请先登录"));
    }

    /**
     * 创建JSON访问拒绝处理器
     * 当用户无权限访问时返回JSON格式的错误响应
     *
     * @return 访问拒绝处理器
     */
    private ServerAccessDeniedHandler jsonAccessDeniedHandler() {
        return (exchange, ex) -> writeJson(exchange, HttpStatus.FORBIDDEN, Response.error(ErrorCode.FORBIDDEN, "无权限访问"));
    }

    /**
     * 将对象写入响应
     * 将指定对象序列化为JSON并写入HTTP响应
     *
     * @param exchange 服务器Web交换对象
     * @param status HTTP状态码
     * @param body 响应体对象
     * @return Mono<Void> 异步操作结果
     */
    private Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, Object body) {
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
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
