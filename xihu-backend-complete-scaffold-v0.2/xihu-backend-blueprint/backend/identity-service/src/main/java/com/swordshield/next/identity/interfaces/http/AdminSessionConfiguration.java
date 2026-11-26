package com.swordshield.next.identity.interfaces.http;

import java.io.IOException;
import java.time.Duration;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.HttpStatus;
import org.springframework.session.FlushMode;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
class AdminSessionConfiguration {

    static final String COOKIE_NAME = "admin_session";
    static final String COOKIE_PATH = "/api/v1";
    static final String ACCOUNT_ID_ATTR = "swordshield.admin.accountId";
    static final String CREDENTIAL_VERSION_ATTR = "swordshield.admin.credentialVersion";

    private static final Logger LOG = LoggerFactory.getLogger(AdminSessionConfiguration.class);

    /** 创建纯 Redis 普通会话仓库，仅设 8h 空闲超时并立即刷盘以确认持久化结果。 */
    @Bean
    RedisSessionRepository adminSessionRepository(RedisConnectionFactory connectionFactory,
            @Value("${identity.admin.session-namespace:xihu:admin:session}") String namespace,
            @Value("${identity.admin.session-idle-timeout:8h}") Duration idleTimeout) {
        var redis = new RedisTemplate<String, Object>();
        redis.setConnectionFactory(connectionFactory);
        redis.setKeySerializer(StringRedisSerializer.UTF_8);
        redis.setHashKeySerializer(StringRedisSerializer.UTF_8);
        redis.setHashValueSerializer(new JdkSerializationRedisSerializer());
        redis.afterPropertiesSet();

        var repository = new RedisSessionRepository(redis);
        repository.setRedisKeyNamespace(namespace);
        repository.setDefaultMaxInactiveInterval(idleTimeout);
        repository.setFlushMode(FlushMode.IMMEDIATE);
        return repository;
    }

    /** 配置 host-only、HttpOnly、SameSite=Lax 的会话级 Admin Session Cookie。 */
    @Bean
    DefaultCookieSerializer adminCookieSerializer(
            @Value("${identity.admin.secure-cookie:false}") boolean secureCookie) {
        var serializer = new DefaultCookieSerializer();
        serializer.setCookieName(COOKIE_NAME);
        serializer.setCookiePath(COOKIE_PATH);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secureCookie);
        serializer.setSameSite("Lax");
        serializer.setUseBase64Encoding(false);
        return serializer;
    }

    @Bean
    CookieHttpSessionIdResolver adminSessionIdResolver(DefaultCookieSerializer adminCookieSerializer) {
        var resolver = new CookieHttpSessionIdResolver();
        resolver.setCookieSerializer(adminCookieSerializer);
        return resolver;
    }

    /** 仅对 Admin 认证与内部校验路径启用 Spring Session，确保 Portal 请求绝不触碰 Admin Session。 */
    @Bean
    FilterRegistrationBean<SessionRepositoryFilter<?>> adminSessionRepositoryFilter(
            RedisSessionRepository repository, CookieHttpSessionIdResolver resolver) {
        var filter = new SessionRepositoryFilter<>(repository);
        filter.setHttpSessionIdResolver(resolver);
        var registration = new FilterRegistrationBean<SessionRepositoryFilter<?>>(filter);
        registration.setName("adminSessionRepositoryFilter");
        registration.addUrlPatterns("/api/v1/auth/admin/*", "/internal/admin/session/*");
        registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
        return registration;
    }

    /** 在 SessionRepositoryFilter 外层拦截存储故障，统一返回 503 AUTH_STATE_UNAVAILABLE 并保留原 Cookie。 */
    @Bean
    FilterRegistrationBean<OncePerRequestFilter> adminStateExceptionFilter(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver errors) {
        var filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {
                try {
                    filterChain.doFilter(request, response);
                } catch (Exception ex) {
                    if (!response.isCommitted() && isStorageUnavailable(ex)) {
                        LOG.warn("Admin 在线认证状态存储暂不可用: {}", ex.getClass().getSimpleName());
                        errors.resolveException(request, response, null,
                                new PortalAuthProblem(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_STATE_UNAVAILABLE",
                                        "服务不可用", "暂时无法确认后台在线认证状态。"));
                        return;
                    }
                    throw ex;
                }
            }
        };
        var registration = new FilterRegistrationBean<OncePerRequestFilter>(filter);
        registration.setName("adminStateExceptionFilter");
        registration.addUrlPatterns("/api/v1/auth/admin/*", "/internal/admin/session/*");
        registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER - 1);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
        return registration;
    }

    static boolean isStorageUnavailable(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof DataAccessException
                    || current.getClass().getName().startsWith("io.lettuce.core.")) {
                return true;
            }
        }
        return false;
    }
}
