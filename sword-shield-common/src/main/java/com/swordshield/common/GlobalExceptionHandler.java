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

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BlockException.class)
    public Response<Object> handleBlockException(BlockException e) {
        return Response.error(ErrorCode.TOO_MANY_REQUESTS, "Too many requests, please try again later");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Response<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        return Response.error(ErrorCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public Response<Object> handleSecurityException(SecurityException e) {
        return Response.error(ErrorCode.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public Response<Object> handleForbiddenException(ForbiddenException e) {
        return Response.error(ErrorCode.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public Response<Object> handleTooManyRequestsException(TooManyRequestsException e) {
        return Response.error(ErrorCode.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(RemoteServiceException.class)
    public Response<Object> handleRemoteServiceException(RemoteServiceException e) {
        log.warn("Remote service error: code={}, message={}", e.getCode(), safeMessage(e));
        return Response.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = firstFieldErrorMessage(e.getBindingResult().getFieldErrors());
        return Response.error(ErrorCode.BAD_REQUEST, message == null ? ErrorCode.BAD_REQUEST.message : message);
    }

    @ExceptionHandler(BindException.class)
    public Response<Object> handleBindException(BindException e) {
        String message = firstFieldErrorMessage(e.getBindingResult().getFieldErrors());
        return Response.error(ErrorCode.BAD_REQUEST, message == null ? ErrorCode.BAD_REQUEST.message : message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Response<Object> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse(ErrorCode.BAD_REQUEST.message);
        return Response.error(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Response<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return Response.error(ErrorCode.BAD_REQUEST, "请求参数格式错误");
    }

    @ExceptionHandler(Exception.class)
    public Response<Object> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception: {}", requestSummary(request), e);
        return Response.error(ErrorCode.SERVER_ERROR);
    }

    private String firstFieldErrorMessage(java.util.List<FieldError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) return null;
        FieldError first = fieldErrors.get(0);
        if (first == null) return null;
        String msg = first.getDefaultMessage();
        return msg == null || msg.isBlank() ? null : msg;
    }

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

    private String safeMessage(Throwable ex) {
        if (ex == null) return null;
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) return null;
        msg = msg.replaceAll("[\\r\\n\\t]+", " ").trim();
        return msg.length() > 300 ? msg.substring(0, 300) : msg;
    }
}
