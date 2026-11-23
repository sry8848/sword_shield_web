package com.swordshield.next.gateway;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Map;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import tools.jackson.databind.json.JsonMapper;

@Component
@Order(-1)
final class GatewayProblemHandler implements ErrorWebExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GatewayProblemHandler.class);

    private final Tracer tracer;
    private final JsonMapper jsonMapper;

    GatewayProblemHandler(Tracer tracer, JsonMapper jsonMapper) {
        this.tracer = tracer;
        this.jsonMapper = jsonMapper;
    }

    /** 将尚未提交的 WebFlux 异常响应写为带请求追踪的 Problem Details。 */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        // 1. 已发送响应头时，将异常交回框架处理。
        var response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(error);
        }

        // WebFlux 恢复订阅上下文后，当前 span 才能稳定关联本次请求。
        return Mono.defer(() -> {
            // 2. 读取 HTTP 状态与公开错误说明，关联真实请求追踪。
            HttpStatusCode status = statusOf(error);
            PublicError publicError = describe(status);
            String traceId = tracer.currentSpan().context().traceId();
            if (status.value() == 500) {
                LOG.error("Gateway 请求发生内部异常", error);
            } else if (status.is5xxServerError()) {
                LOG.warn("Gateway 请求失败，错误码 {}", publicError.code());
            }

            // 3. 保留框架错误携带的协议头，写入规范的错误响应。
            response.setStatusCode(status);
            if (error instanceof ErrorResponse frameworkError) {
                response.getHeaders().putAll(frameworkError.getHeaders());
            }
            response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
            byte[] payload = jsonMapper.writeValueAsBytes(Map.of(
                    "type", "about:blank", "title", publicError.title(), "status", status.value(),
                    "detail", publicError.detail(), "code", publicError.code(), "traceId", traceId,
                    "timestamp", Instant.now().toString()));
            return response.writeWith(Mono.just(response.bufferFactory().wrap(payload)));
        });
    }

    /** 已知连接异常转换为网关状态，WebFlux 自身的异常保留其状态。 */
    private static HttpStatusCode statusOf(Throwable error) {
        if (error instanceof ErrorResponse frameworkError) {
            return frameworkError.getStatusCode();
        }
        if (error instanceof ConnectException || error instanceof UnknownHostException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (error instanceof PrematureCloseException) {
            return HttpStatus.BAD_GATEWAY;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /** 使用工程规范定义的固定标题、错误码及可公开说明。 */
    private static PublicError describe(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> new PublicError("INVALID_ARGUMENT", "请求错误", "请求格式或参数错误");
            case 404 -> new PublicError("RESOURCE_NOT_FOUND", "未找到", "目标资源不存在");
            case 405 -> new PublicError("METHOD_NOT_ALLOWED", "不支持的请求方法", "目标资源不支持该请求方法");
            case 502 -> new PublicError("BAD_GATEWAY", "网关错误", "网关收到无效的上游响应");
            case 503 -> new PublicError("SERVICE_UNAVAILABLE", "服务不可用", "服务暂不可用");
            case 504 -> new PublicError("GATEWAY_TIMEOUT", "网关超时", "网关等待上游响应超时");
            default -> status.is4xxClientError()
                    ? new PublicError("HTTP_ERROR", "请求失败", "请求失败")
                    : new PublicError("INTERNAL_ERROR", "服务器内部错误", "服务器内部错误");
        };
    }

    private record PublicError(String code, String title, String detail) {
    }
}
