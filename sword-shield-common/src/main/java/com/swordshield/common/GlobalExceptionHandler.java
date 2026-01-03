package com.swordshield.common;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一处理系统中各种异常，返回标准化的错误响应
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理流量控制异常
     * @param e 流量控制异常对象
     * @return 错误响应对象
     */
    @ExceptionHandler(BlockException.class)
    public Response<Object> handleBlockException(BlockException e) {
        return Response.error(ErrorCode.TOO_MANY_REQUESTS, "Too many requests, please try again later");
    }

    /**
     * 处理非法参数异常
     * @param e 非法参数异常对象
     * @return 错误响应对象
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Response<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        return Response.error(ErrorCode.BAD_REQUEST, e.getMessage());
    }

    /**
     * 处理安全异常
     * @param e 安全异常对象
     * @return 错误响应对象
     */
    @ExceptionHandler(SecurityException.class)
    public Response<Object> handleSecurityException(SecurityException e) {
        return Response.error(ErrorCode.UNAUTHORIZED, e.getMessage());
    }

    /**
     * 处理权限不足异常
     * @param e 权限不足异常对象
     * @return 错误响应对象
     */
    @ExceptionHandler(ForbiddenException.class)
    public Response<Object> handleForbiddenException(ForbiddenException e) {
        return Response.error(ErrorCode.FORBIDDEN, e.getMessage());
    }

    /**
     * 处理请求过多异常
     * @param e 请求过多异常对象
     * @return 错误响应对象
     */
    @ExceptionHandler(TooManyRequestsException.class)
    public Response<Object> handleTooManyRequestsException(TooManyRequestsException e) {
        return Response.error(ErrorCode.TOO_MANY_REQUESTS, e.getMessage());
    }

    /**
     * 处理远程服务异常
     * @param e 远程服务异常对象
     * @return 错误响应对象
     */
    @ExceptionHandler(RemoteServiceException.class)
    public Response<Object> handleRemoteServiceException(RemoteServiceException e) {
        log.warn("Remote service error: code={}, message={}", e.getCode(), safeMessage(e));
        return Response.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理方法参数验证异常
     * @param e 方法参数验证异常对象
     * @return 错误响应对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = firstFieldErrorMessage(e.getBindingResult().getFieldErrors());
        return Response.error(ErrorCode.BAD_REQUEST, message == null ? ErrorCode.BAD_REQUEST.message : message);
    }

    /**
     * 处理绑定异常
     * @param e 绑定异常对象
     * @return 错误响应对象
     */
    @ExceptionHandler(BindException.class)
    public Response<Object> handleBindException(BindException e) {
        String message = firstFieldErrorMessage(e.getBindingResult().getFieldErrors());
        return Response.error(ErrorCode.BAD_REQUEST, message == null ? ErrorCode.BAD_REQUEST.message : message);
    }

    /**
     * 处理约束违反异常
     * @param e 约束违反异常对象
     * @return 错误响应对象
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Response<Object> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse(ErrorCode.BAD_REQUEST.message);
        return Response.error(ErrorCode.BAD_REQUEST, message);
    }

    /**
     * 处理HTTP消息不可读异常
     * @param e HTTP消息不可读异常对象
     * @return 错误响应对象
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Response<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return Response.error(ErrorCode.BAD_REQUEST, "请求参数格式错误");
    }

    /**
     * 处理通用异常
     * @param e 异常对象
     * @param request HTTP请求对象
     * @return 错误响应对象
     */
    @ExceptionHandler(Exception.class)
    public Response<Object> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception: {}", requestSummary(request), e);
        return Response.error(ErrorCode.SERVER_ERROR);
    }

    /**
     * 获取第一个字段错误消息
     * @param fieldErrors 字段错误列表
     * @return 第一个字段错误消息，如果没有错误则返回null
     */
    private String firstFieldErrorMessage(java.util.List<FieldError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) return null;
        FieldError first = fieldErrors.get(0);
        if (first == null) return null;
        String msg = first.getDefaultMessage();
        return msg == null || msg.isBlank() ? null : msg;
    }

    /**
     * 获取请求摘要信息
     * @param request HTTP请求对象
     * @return 请求摘要字符串
     */
    private String requestSummary(HttpServletRequest request) {
        if (request == null) return "request=unknown";
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String qs = request.getQueryString();
        String remote = request.getRemoteAddr();
        StringBuilder sb = new StringBuilder();
        sb.append("method=").append(method == null ? "?" : method);
        sb.append(", uri=").append(uri == null ? "?" : uri);
        if (qs != null && !qs.isBlank()) {
            sb.append("?").append(qs);
        }
        if (remote != null && !remote.isBlank()) {
            sb.append(", remote=").append(remote.trim());
        }
        return sb.toString();
    }

    /**
     * 获取安全的消息字符串
     * @param ex 异常对象
     * @return 安全的消息字符串
     */
    private String safeMessage(Throwable ex) {
        if (ex == null) return null;
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) return null;
        msg = msg.replaceAll("[\\r\\n\\t]+", " ").trim();
        return msg.length() > 300 ? msg.substring(0, 300) : msg;
    }
}
