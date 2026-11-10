
package com.swordshield.next.identity;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.swordshield.next.common.BusinessException;
import com.swordshield.next.common.FailureKind;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** 将 Identity 的 MVC 异常转换为项目约定的 Problem Details。 */
@RestControllerAdvice
class IdentityMvcExceptionHandler extends ResponseEntityExceptionHandler implements WebMvcConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(IdentityMvcExceptionHandler.class);

    private static final URI ABOUT_BLANK = URI.create("about:blank");

    private final Tracer tracer;
    private final ContentNegotiationManager contentNegotiationManager;

    /** 注入请求追踪和 MVC 内容协商组件。 */
    IdentityMvcExceptionHandler(Tracer tracer, @Lazy ContentNegotiationManager contentNegotiationManager) {
        this.tracer = tracer;
        // MVC 创建协商组件前会收集 WebMvcConfigurer，因此等到处理请求时再解析该依赖。
        this.contentNegotiationManager = contentNegotiationManager;
    }

    /** 将业务失败映射为对应状态，并使用统一的错误响应格式。 */
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Object> handleBusiness(BusinessException exception, WebRequest request) {
        return handleExceptionInternal(exception, null, HttpHeaders.EMPTY, statusOf(exception.getKind()), request);
    }

    /** 返回业务失败类别对应的 HTTP 状态。 */
    private static HttpStatusCode statusOf(FailureKind kind) {
        return switch (kind) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case RULE_VIOLATION -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
    }

    /** 接收 MVC 内置处理器未识别的异常，返回通用 500 响应。 */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
        return handleExceptionInternal(exception, null, HttpHeaders.EMPTY, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /** 根据 MVC 已解析的 HTTP 状态统一构造公开错误，并保留原响应头。 */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // 1. 内容协商失败时，按 MVC 的协商结果决定能否返回错误 JSON。
        // Spring 的 Problem Details 序列化会尝试回退到 Problem JSON，此处保留明确拒绝 JSON 的 406。
        if (status.value() == 406 && !acceptsProblemJson((NativeWebRequest) request)) {
            return createResponseEntity(null, headers, status, request);
        }

        // 2. 按状态选择公开标题、通用码和说明，并为业务失败保留原有业务码及说明。
        ProblemDetail problem = switch (status.value()) {
            case 400 -> problem(status, "INVALID_ARGUMENT", "请求错误",
                    exception instanceof HttpMessageNotReadableException
                            ? "请求体格式不正确" : "请求参数不合法");
            case 401 -> problem(status, "UNAUTHENTICATED", "未认证", "缺少有效的认证凭证");
            case 403 -> problem(status, "FORBIDDEN", "禁止访问", "无权访问目标资源");
            case 404 -> problem(status, "RESOURCE_NOT_FOUND", "未找到", "目标资源不存在");
            case 405 -> problem(status, "METHOD_NOT_ALLOWED", "不支持的请求方法", "目标资源不支持该请求方法");
            case 409 -> problem(status, "BUSINESS_CONFLICT", "状态冲突", "当前状态与该操作冲突");
            case 413 -> problem(status, "PAYLOAD_TOO_LARGE", "请求内容过大", "请求体超过允许的大小");
            case 415 -> problem(status, "UNSUPPORTED_MEDIA_TYPE", "不支持的媒体类型", "请求体媒体类型不受支持");
            case 422 -> problem(status, "BUSINESS_RULE_VIOLATION", "无法处理的内容", "请求不满足业务规则");
            case 429 -> problem(status, "TOO_MANY_REQUESTS", "请求过于频繁", "请求频率超过限制");
            case 502 -> problem(status, "BAD_GATEWAY", "网关错误", "网关收到无效的上游响应");
            case 503 -> problem(status, "SERVICE_UNAVAILABLE", "服务不可用", "目标服务暂不可用");
            case 504 -> problem(status, "GATEWAY_TIMEOUT", "网关超时", "网关等待上游响应超时");
            default -> {
                if (status.is5xxServerError()) {
                    // 原始异常只在最终处理边界写入日志，响应使用公开说明。
                    LOG.error("Identity MVC 未预期异常", exception);
                    yield problem(status, "INTERNAL_ERROR", "服务器内部错误", "服务器内部错误");
                }
                yield status.is4xxClientError() ? problem(status, "HTTP_ERROR", "请求失败", "无法完成请求") : null;
            }
        };
        if (problem == null) {
            return super.handleExceptionInternal(exception, body, headers, status, request);
        }
        if (exception instanceof BusinessException business) {
            problem.setProperty("code", business.getCode());
            problem.setDetail(business.getMessage());
        }

        // 3. 为参数校验附加公开的字段错误。
        if (status.value() == 400) {
            var errors = validationErrors(exception);
            if (!errors.isEmpty()) {
                problem.setProperty("errors", errors);
            }
        }

        // 4. 保留 Allow 等 MVC 响应头，并指定错误响应的媒体类型。
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.addAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return super.handleExceptionInternal(exception, problem, responseHeaders, status, request);
    }

    /** 根据 MVC 内容协商结果判断 406 能否携带 Problem JSON。 */
    private boolean acceptsProblemJson(NativeWebRequest request) {
        try {
            // 更具体的类型声明优先于通配类型，包括通过 q=0 明确排除 Problem JSON。
            return contentNegotiationManager.resolveMediaTypes(request).stream()
                    .filter(type -> type.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .reduce((best, candidate) -> candidate.removeQualityValue().isMoreSpecific(best.removeQualityValue())
                            ? candidate : best)
                    .filter(type -> type.getQualityValue() > 0)
                    .isPresent();
        } catch (HttpMediaTypeNotAcceptableException invalidAccept) {
            return false;
        }
    }

    /** 创建指定 HTTP 状态的错误响应，附上当前请求追踪 ID 和生成时间。 */
    private ProblemDetail problem(HttpStatusCode status, String code, String title, String detail) {
        // 1. 读取当前请求的真实 span。
        var span = tracer.currentSpan();
        if (span == null) {
            throw new IllegalStateException("当前请求追踪上下文缺少 span");
        }

        // 2. 设置 RFC 9457 标准字段和项目扩展字段。
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(ABOUT_BLANK);
        problem.setTitle(title);
        problem.setProperty("code", code);
        problem.setProperty("traceId", span.context().traceId());
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    /** 提取请求体及方法参数的字段错误，保留适用的校验说明。 */
    private static List<Map<String, String>> validationErrors(Exception exception) {
        // 1. 处理请求体等对象绑定后的字段校验结果。
        if (exception instanceof MethodArgumentNotValidException invalid) {
            return invalid.getFieldErrors().stream()
                    .map(IdentityMvcExceptionHandler::fieldError)
                    .toList();
        }

        // 2. 处理 MVC 直接校验的方法参数，包括组合校验中的对象字段。
        List<Map<String, String>> errors = new ArrayList<>();
        if (exception instanceof HandlerMethodValidationException invalid) {
            for (var result : invalid.getParameterValidationResults()) {
                if (result instanceof ParameterErrors bindingErrors) {
                    for (FieldError error : bindingErrors.getFieldErrors()) {
                        errors.add(fieldError(error));
                    }
                } else {
                    String field = parameterName(result.getMethodParameter());
                    for (var error : result.getResolvableErrors()) {
                        String[] codes = error.getCodes();
                        errors.add(fieldError(field, codes[codes.length - 1], error.getDefaultMessage()));
                    }
                }
            }
        }
        return errors;
    }

    /** 取请求参数对外声明的名称，省略别名时使用方法参数名。 */
    private static String parameterName(MethodParameter parameter) {
        // 1. 使用 MVC 已合并的注解别名。
        var query = parameter.getParameterAnnotation(RequestParam.class);
        if (query != null && !query.name().isEmpty()) {
            return query.name();
        }
        var path = parameter.getParameterAnnotation(PathVariable.class);
        if (path != null && !path.name().isEmpty()) {
            return path.name();
        }
        return parameter.getParameterName();
    }

    /** 提取绑定对象的字段名、约束类型和公开说明。 */
    private static Map<String, String> fieldError(FieldError error) {
        // 绑定失败的原始消息可能包含输入值或底层转换细节。
        String message = error.isBindingFailure() ? null : error.getDefaultMessage();
        return fieldError(error.getField(), error.getCode(), message);
    }

    /** 将约束类型和可公开的校验说明转换为字段错误。 */
    private static Map<String, String> fieldError(String field, String constraint, String message) {
        // 1. 区分必填约束和其他字段约束，在缺少说明时使用通用文案。
        boolean required = "NotBlank".equals(constraint) || "NotNull".equals(constraint) || "NotEmpty".equals(constraint);
        String fallback = required ? "缺少必填值" : "字段值不符合要求";
        String detail = message != null && !message.isBlank() ? message : fallback;
        return Map.of("field", field, "code", required ? "REQUIRED" : "INVALID_VALUE", "detail", detail);
    }

    /** 注册 Spring MVC 序列化前的 Problem Details 拦截器。 */
    @Override
    public void addErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
        // MVC 在调用拦截器前会填入请求路径。Identity 当前没有实际的错误实例 URI。
        interceptors.add((problem, response) -> problem.setInstance(null));
    }
}
