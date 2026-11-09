
package com.swordshield.next.identity;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
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

    /** 使用 Boot 自动配置的 tracer 读取当前请求 span。 */
    IdentityMvcExceptionHandler(Tracer tracer) {
        this.tracer = tracer;
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
        // 1. 使用本阶段约定的公开标题和说明，未覆盖的状态交还 Spring。
        ProblemDetail problem = switch (status.value()) {
            case 400 -> problem(status, "INVALID_ARGUMENT", "请求错误",
                    exception instanceof HttpMessageNotReadableException
                            ? "请求体格式不正确" : "请求参数不合法");
            case 404 -> problem(status, "RESOURCE_NOT_FOUND", "未找到", "目标资源不存在");
            case 405 -> problem(status, "METHOD_NOT_ALLOWED", "不支持的请求方法", "目标资源不支持该请求方法");
            case 415 -> problem(status, "UNSUPPORTED_MEDIA_TYPE", "不支持的媒体类型", "请求体媒体类型不受支持");
            case 500 -> problem(status, "INTERNAL_ERROR", "服务器内部错误", "服务器内部错误");
            default -> null;
        };
        if (problem == null) {
            return super.handleExceptionInternal(exception, body, headers, status, request);
        }

        // 2. 附加安全的字段校验结果，或为未预期异常写一条堆栈日志。
        if (status.value() == 400) {
            var errors = validationErrors(exception);
            if (!errors.isEmpty()) {
                problem.setProperty("errors", errors);
            }
        } else if (status.value() == 500) {
            LOG.error("Identity MVC 未预期异常", exception);
        }

        // 3. 保留 Allow 等 MVC 响应头，并指定错误响应的媒体类型。
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.addAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return super.handleExceptionInternal(exception, problem, responseHeaders, status, request);
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

    /** 提取请求体及方法参数的字段错误，不读取拒绝值或校验消息。 */
    private static List<Map<String, String>> validationErrors(Exception exception) {
        // 1. 处理请求体等对象绑定后的字段校验结果。
        if (exception instanceof MethodArgumentNotValidException invalid) {
            return invalid.getFieldErrors().stream()
                    .map(error -> fieldError(error.getField(), error.getCode()))
                    .toList();
        }

        // 2. 处理 MVC 直接校验的方法参数，包括组合校验中的对象字段。
        List<Map<String, String>> errors = new ArrayList<>();
        if (exception instanceof HandlerMethodValidationException invalid) {
            for (var result : invalid.getParameterValidationResults()) {
                if (result instanceof ParameterErrors bindingErrors) {
                    for (FieldError error : bindingErrors.getFieldErrors()) {
                        errors.add(fieldError(error.getField(), error.getCode()));
                    }
                } else {
                    String field = parameterName(result.getMethodParameter());
                    for (var error : result.getResolvableErrors()) {
                        String[] codes = error.getCodes();
                        errors.add(fieldError(field, codes[codes.length - 1]));
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

    /** 根据约束类型生成固定的公开字段原因码及说明。 */
    private static Map<String, String> fieldError(String field, String constraint) {
        // 1. 将必填约束与其他字段约束映射为固定文案。
        boolean required = "NotBlank".equals(constraint) || "NotNull".equals(constraint) || "NotEmpty".equals(constraint);
        return Map.of("field", field, "code", required ? "REQUIRED" : "INVALID_VALUE",
                "detail", required ? "缺少必填值" : "字段值不符合要求");
    }

    /** 注册 Spring MVC 序列化前的 Problem Details 拦截器。 */
    @Override
    public void addErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
        // MVC 在调用拦截器前会填入请求路径。Identity 当前没有实际的错误实例 URI。
        interceptors.add((problem, response) -> problem.setInstance(null));
    }
}
