package com.swordshield.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 访问日志过滤器
 * 用于记录HTTP请求的访问日志，包括请求方法、路径、响应状态码和处理耗时
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@ConditionalOnProperty(prefix = "app.access-log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AccessLogFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    /**
     * 判断是否不应该过滤该请求
     * @param request HTTP请求对象
     * @return 如果请求URI以/actuator、/swagger-ui或/v3/api-docs开头则返回true，否则返回false
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && (uri.startsWith("/actuator") || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs"));
    }

    /**
     * 执行内部过滤逻辑，记录请求访问日志
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 记录请求开始时间
        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 计算请求处理耗时（毫秒）
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            String method = request.getMethod();
            String path = request.getRequestURI();
            int status = response.getStatus();
            // 记录访问日志
            log.info("HTTP {} {} -> {} ({} ms)", method, path, status, durationMs);
        }
    }
}

