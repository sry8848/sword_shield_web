package com.swordshield.common;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
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

    @ExceptionHandler(RemoteServiceException.class)
    public Response<Object> handleRemoteServiceException(RemoteServiceException e) {
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
    public Response<Object> handleException(Exception e) {
        return Response.error(ErrorCode.SERVER_ERROR);
    }

    private String firstFieldErrorMessage(java.util.List<FieldError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) return null;
        FieldError first = fieldErrors.get(0);
        if (first == null) return null;
        String msg = first.getDefaultMessage();
        return msg == null || msg.isBlank() ? null : msg;
    }
}
