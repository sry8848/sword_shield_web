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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String headerTraceParent = request.getHeader(TraceUtils.TRACEPARENT_HEADER);

        String traceId = TraceUtils.currentOtelTraceId()
                .or(() -> TraceUtils.traceIdFromTraceParent(headerTraceParent))
                .orElseGet(() -> TraceUtils.ensureW3cTraceId(request.getHeader(TraceUtils.TRACE_ID_HEADER)));

        String traceParent = TraceUtils.currentTraceParent()
                .orElseGet(() -> TraceUtils.isValidTraceParent(headerTraceParent) ? headerTraceParent.trim() : TraceUtils.newTraceParent(traceId));

        HttpServletRequest requestToUse = new TraceHeadersRequestWrapper(request, traceParent, traceId);

        String existingTraceId = MDC.get(TraceUtils.TRACE_ID_MDC_KEY);
        String existingOtelTraceId = MDC.get(TraceUtils.OTEL_TRACE_ID_MDC_KEY);

        boolean setTraceIdByThisFilter = !StringUtils.hasText(existingTraceId);
        boolean setOtelTraceIdByThisFilter = !StringUtils.hasText(existingOtelTraceId);

        if (setTraceIdByThisFilter) {
            MDC.put(TraceUtils.TRACE_ID_MDC_KEY, traceId);
        }
        if (setOtelTraceIdByThisFilter) {
            MDC.put(TraceUtils.OTEL_TRACE_ID_MDC_KEY, traceId);
        }
        response.setHeader(TraceUtils.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(requestToUse, response);
        } finally {
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

    private static final class TraceHeadersRequestWrapper extends HttpServletRequestWrapper {
        private final String traceParent;
        private final String traceId;

        private TraceHeadersRequestWrapper(HttpServletRequest request, String traceParent, String traceId) {
            super(request);
            this.traceParent = traceParent;
            this.traceId = traceId;
        }

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
