package com.swordshield.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swordshield.common.ErrorCode;
import com.swordshield.common.Response;
import com.swordshield.common.TraceUtils;
import com.swordshield.gateway.GatewayTraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayErrorWebExceptionHandler implements WebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GatewayErrorWebExceptionHandler.class);

    private final ObjectMapper objectMapper;
    private final GatewayTraceContext traceContext;

    public GatewayErrorWebExceptionHandler(ObjectMapper objectMapper, GatewayTraceContext traceContext) {
        this.objectMapper = objectMapper;
        this.traceContext = traceContext;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        String message = resolveMessage(ex, status);
        logException(exchange, ex, status, message);

        Response<Object> body = Response.error(ErrorCode.fromHttpStatus(status.value()), message);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"code\":" + status.value() + ",\"message\":\"" + escapeJson(message) + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setContentLength(bytes.length);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private void logException(ServerWebExchange exchange, Throwable ex, HttpStatus status, String message) {
        String method = exchange.getRequest().getMethod() == null ? "?" : exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getRawPath();

        GatewayTraceContext.TraceInfo traceInfo = traceContext == null ? null : traceContext.ensureTraceInfo(exchange);
        String traceId = traceInfo == null ? null : traceInfo.traceId();
        boolean setTraceId = traceId != null && !traceId.isBlank() && MDC.get(TraceUtils.TRACE_ID_MDC_KEY) == null;
        boolean setOtelTraceId = traceId != null && !traceId.isBlank() && MDC.get(TraceUtils.OTEL_TRACE_ID_MDC_KEY) == null;
        if (setTraceId) {
            MDC.put(TraceUtils.TRACE_ID_MDC_KEY, traceId);
        }
        if (setOtelTraceId) {
            MDC.put(TraceUtils.OTEL_TRACE_ID_MDC_KEY, traceId);
        }
        try {
            if (status.is5xxServerError()) {
                log.error("GW {} {} -> {} {}", method, path, status.value(), message, ex);
                return;
            }
            if (status == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("GW {} {} -> {} {}", method, path, status.value(), message);
                return;
            }
            if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
                log.info("GW {} {} -> {}", method, path, status.value());
                return;
            }
            log.warn("GW {} {} -> {} {}", method, path, status.value(), message);
        } finally {
            if (setOtelTraceId) {
                MDC.remove(TraceUtils.OTEL_TRACE_ID_MDC_KEY);
            }
            if (setTraceId) {
                MDC.remove(TraceUtils.TRACE_ID_MDC_KEY);
            }
        }
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return HttpStatus.valueOf(rse.getStatusCode().value());
        }
        if (ex instanceof NotFoundException nfe) {
            // gateway 找不到服务实例/路由时通常会抛该异常
            org.springframework.http.HttpStatusCode code = nfe.getStatusCode();
            return HttpStatus.valueOf(code == null ? HttpStatus.SERVICE_UNAVAILABLE.value() : code.value());
        }
        if (ex instanceof WebClientRequestException) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (ex instanceof TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (hasCause(ex, TimeoutException.class)) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (hasCause(ex, java.net.ConnectException.class) || hasCause(ex, java.net.SocketTimeoutException.class)) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (hasCause(ex, io.netty.handler.timeout.ReadTimeoutException.class) ||
                hasCause(ex, io.netty.handler.timeout.WriteTimeoutException.class)) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(Throwable ex, HttpStatus status) {
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            String msg = safeMessage(ex);
            return msg == null ? "Service unavailable" : "Service unavailable: " + msg;
        }
        if (status == HttpStatus.BAD_GATEWAY) {
            String msg = safeMessage(ex);
            return msg == null ? "Bad gateway" : "Bad gateway: " + msg;
        }
        if (status == HttpStatus.GATEWAY_TIMEOUT) {
            return "Gateway timeout";
        }
        if (status == HttpStatus.UNAUTHORIZED) {
            return "未登录，请先登录";
        }
        if (status == HttpStatus.FORBIDDEN) {
            return "无权限访问";
        }
        String msg = safeMessage(ex);
        return msg == null ? "Internal server error" : msg;
    }

    private String safeMessage(Throwable ex) {
        if (ex == null) return null;
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) return null;
        // 避免把过长堆栈/敏感信息直接回显到客户端
        msg = msg.replaceAll("[\\r\\n\\t]+", " ").trim();
        return msg.length() > 300 ? msg.substring(0, 300) : msg;
    }

    private boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
        Throwable cur = ex;
        int depth = 0;
        while (cur != null && depth++ < 12) {
            if (type.isInstance(cur)) return true;
            cur = cur.getCause();
        }
        return false;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
