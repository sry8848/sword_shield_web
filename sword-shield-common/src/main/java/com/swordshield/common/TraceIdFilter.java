package com.swordshield.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = TraceUtils.currentOtelTraceId()
                .orElseGet(() -> TraceUtils.ensureTraceId(request.getHeader(TraceUtils.TRACE_ID_HEADER)));

        String existing = MDC.get(TraceUtils.TRACE_ID_MDC_KEY);
        boolean setByThisFilter = !StringUtils.hasText(existing);
        if (setByThisFilter) {
            MDC.put(TraceUtils.TRACE_ID_MDC_KEY, traceId);
        }
        response.setHeader(TraceUtils.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (setByThisFilter) {
                String current = MDC.get(TraceUtils.TRACE_ID_MDC_KEY);
                if (traceId.equals(current)) {
                    MDC.remove(TraceUtils.TRACE_ID_MDC_KEY);
                }
            }
        }
    }
}
