package com.swordshield.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public Response<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        return Response.error(ErrorCode.BAD_REQUEST.code, e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public Response<Object> handleSecurityException(SecurityException e) {
        return Response.error(ErrorCode.UNAUTHORIZED.code, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Response<Object> handleException(Exception e) {
        return Response.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorCode.SERVER_ERROR.message);
    }
}

