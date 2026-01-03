package com.swordshield.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 追踪ID过滤器，用于在HTTP请求中处理和传播分布式追踪ID
 * 该过滤器实现了W3C Trace Context规范，确保在整个请求链路中保持一致的追踪ID
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /**
     * 内部过滤方法，处理请求中的追踪ID逻辑
     *
     * @param request     HTTP请求对象
     * @param response    HTTP响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException      IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 从请求头中获取traceparent值
        String headerTraceParent = request.getHeader(TraceUtils.TRACEPARENT_HEADER);

        // 获取或生成traceId，优先级：当前OTel traceId -> 从traceparent解析 -> 确保W3C格式的traceId
        String traceId = TraceUtils.currentOtelTraceId()
                .or(() -> TraceUtils.traceIdFromTraceParent(headerTraceParent))
                .orElseGet(() -> TraceUtils.ensureW3cTraceId(request.getHeader(TraceUtils.TRACE_ID_HEADER)));

        // 获取或生成traceParent，优先级：当前traceParent -> 验证后的headerTraceParent -> 基于traceId生成新的traceParent
        String traceParent = TraceUtils.currentTraceParent()
                .orElseGet(() -> TraceUtils.isValidTraceParent(headerTraceParent) ? headerTraceParent.trim() : TraceUtils.newTraceParent(traceId));

        // 创建包装请求，将traceParent和traceId注入到请求头中
        HttpServletRequest requestToUse = new TraceHeadersRequestWrapper(request, traceParent, traceId);

        // 检查MDC中是否已存在traceId和otelTraceId
        String existingTraceId = MDC.get(TraceUtils.TRACE_ID_MDC_KEY);
        String existingOtelTraceId = MDC.get(TraceUtils.OTEL_TRACE_ID_MDC_KEY);

        // 判断是否由当前过滤器设置traceId和otelTraceId
        boolean setTraceIdByThisFilter = !StringUtils.hasText(existingTraceId);
        boolean setOtelTraceIdByThisFilter = !StringUtils.hasText(existingOtelTraceId);

        // 将traceId和otelTraceId放入MDC上下文中，用于日志追踪
        if (setTraceIdByThisFilter) {
            MDC.put(TraceUtils.TRACE_ID_MDC_KEY, traceId);
        }
        if (setOtelTraceIdByThisFilter) {
            MDC.put(TraceUtils.OTEL_TRACE_ID_MDC_KEY, traceId);
        }
        // 将traceId设置到响应头中
        response.setHeader(TraceUtils.TRACE_ID_HEADER, traceId);
        try {
            // 执行过滤器链
            filterChain.doFilter(requestToUse, response);
        } finally {
            // 在请求完成后清理MDC中的traceId和otelTraceId
            if (setOtelTraceIdByThisFilter) {
                String current = MDC.get(TraceUtils.OTEL_TRACE_ID_MDC_KEY);
                if (traceId.equals(current)) {
                    MDC.remove(TraceUtils.OTEL_TRACE_ID_MDC_KEY);
                }
            }
            if (setTraceIdByThisFilter) {
                String current = MDC.get(TraceUtils.TRACE_ID_MDC_KEY);
                if (traceId.equals(current)) {
                    MDC.remove(TraceUtils.TRACE_ID_MDC_KEY);
                }
            }
        }
    }

    /**
     * 追踪头请求包装器，用于在请求中注入traceParent和traceId头信息
     * 该包装器会覆盖原始请求的头信息获取方法，确保追踪相关的头信息正确传递
     */
    private static final class TraceHeadersRequestWrapper extends HttpServletRequestWrapper {
        private final String traceParent;
        private final String traceId;

        /**
         * 构造方法
         *
         * @param request      原始HTTP请求
         * @param traceParent  traceParent值
         * @param traceId      traceId值
         */
        private TraceHeadersRequestWrapper(HttpServletRequest request, String traceParent, String traceId) {
            super(request);
            this.traceParent = traceParent;
            this.traceId = traceId;
        }

        /**
         * 获取指定名称的请求头值
         * 对于traceparent和traceId头，返回包装器中存储的值
         *
         * @param name 头名称
         * @return 头值
         */
        @Override
        public String getHeader(String name) {
            if (TraceUtils.TRACEPARENT_HEADER.equalsIgnoreCase(name)) {
                return traceParent;
            }
            if (TraceUtils.TRACE_ID_HEADER.equalsIgnoreCase(name)) {
                return traceId;
            }
            return super.getHeader(name);
        }

        /**
         * 获取指定名称的所有请求头值
         * 对于traceparent和traceId头，返回包装器中存储的值
         *
         * @param name 头名称
         * @return 头值枚举
         */
        @Override
        public Enumeration<String> getHeaders(String name) {
            if (TraceUtils.TRACEPARENT_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(Collections.singletonList(traceParent));
            }
            if (TraceUtils.TRACE_ID_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(Collections.singletonList(traceId));
            }
            return super.getHeaders(name);
        }

        /**
         * 获取所有请求头名称的枚举
         * 确保traceparent和traceId头名称包含在返回的枚举中
         *
         * @return 头名称枚举
         */
        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> base = super.getHeaderNames();
            while (base.hasMoreElements()) {
                names.add(base.nextElement());
            }
            names.add(TraceUtils.TRACEPARENT_HEADER);
            names.add(TraceUtils.TRACE_ID_HEADER);
            return Collections.enumeration(names);
        }
    }
}
