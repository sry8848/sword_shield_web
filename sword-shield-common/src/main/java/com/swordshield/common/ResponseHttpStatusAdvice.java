package com.swordshield.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Optional: map {@link Response#getCode()} to real HTTP status code so that metrics/alerts can use HTTP semantics.
 * <p>
 * Default behavior keeps HTTP 200 for compatibility. Enable via:
 * <pre>
 * app.http.use-status-code=true
 * </pre>
 */
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ResponseBodyAdvice.class)
@ConditionalOnProperty(prefix = "app.http", name = "use-status-code", havingValue = "true")
public class ResponseHttpStatusAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (!(body instanceof Response<?> r)) {
            return body;
        }

        int code = r.getCode();
        if (code == ErrorCode.OK.code || code < 100) {
            return body;
        }
        HttpStatus status = HttpStatus.resolve(code);
        if (status != null) {
            response.setStatusCode(status);
        }
        return body;
    }
}

