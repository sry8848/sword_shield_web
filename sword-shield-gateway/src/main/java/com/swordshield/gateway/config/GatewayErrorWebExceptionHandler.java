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

/**
 * 网关错误处理异常处理器
 * 负责处理网关层的异常，统一返回格式化的错误响应
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayErrorWebExceptionHandler implements WebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GatewayErrorWebExceptionHandler.class);

    private final ObjectMapper objectMapper;
    private final GatewayTraceContext traceContext;

    /**
     * 构造函数
     * @param objectMapper JSON对象映射器，用于序列化响应体
     * @param traceContext 链路追踪上下文，用于获取和设置追踪信息
     */
    public GatewayErrorWebExceptionHandler(ObjectMapper objectMapper, GatewayTraceContext traceContext) {
        this.objectMapper = objectMapper;
        this.traceContext = traceContext;
    }

    /**
     * 处理异常并返回统一格式的错误响应
     * @param exchange 服务器Web交换对象
     * @param ex 抛出的异常对象
     * @return Mono<Void> 异步处理结果
     */
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

    /**
     * 记录异常日志，根据状态码级别选择不同的日志级别
     * 同时处理链路追踪ID的设置和清理
     * @param exchange 服务器Web交换对象
     * @param ex 抛出的异常对象
     * @param status HTTP状态码
     * @param message 异常消息
     */
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

    /**
     * 根据异常类型解析对应的HTTP状态码
     * @param ex 抛出的异常对象
     * @return HttpStatus 对应的HTTP状态码
     */
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

    /**
     * 根据异常和状态码解析错误消息
     * @param ex 抛出的异常对象
     * @param status HTTP状态码
     * @return String 解析后的错误消息
     */
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

    /**
     * 安全获取异常消息，避免返回过长或敏感信息
     * @param ex 异常对象
     * @return String 安全的异常消息
     */
    private String safeMessage(Throwable ex) {
        if (ex == null) return null;
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) return null;
        // 避免把过长堆栈/敏感信息直接回显到客户端
        msg = msg.replaceAll("[\\r\\n\\t]+", " ").trim();
        return msg.length() > 300 ? msg.substring(0, 300) : msg;
    }

    /**
     * 检查异常链中是否包含指定类型的异常
     * @param ex 异常对象
     * @param type 要检查的异常类型
     * @return boolean 是否包含指定类型的异常
     */
    private boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
        Throwable cur = ex;
        int depth = 0;
        while (cur != null && depth++ < 12) {
            if (type.isInstance(cur)) return true;
            cur = cur.getCause();
        }
        return false;
    }

    /**
     * 转义JSON字符串中的特殊字符
     * @param s 原始字符串
     * @return String 转义后的字符串
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
