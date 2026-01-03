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
 * 响应HTTP状态码处理增强类
 * 可选功能：将{@link Response#getCode()}映射到真实的HTTP状态码，以便指标/警报可以使用HTTP语义
 * 默认行为保持HTTP 200以保持兼容性。通过以下配置启用：
 * <pre>
 * app.http.use-status-code=true
 * </pre>
 */
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ResponseBodyAdvice.class)
@ConditionalOnProperty(prefix = "app.http", name = "use-status-code", havingValue = "true")
public class ResponseHttpStatusAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 判断是否支持对指定返回类型和转换器类型的响应体进行处理
     *
     * @param returnType 返回类型参数
     * @param converterType 消息转换器类型
     * @return 总是返回true，表示支持所有类型
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    /**
     * 在响应体写入前进行处理，将Response对象中的code映射为HTTP状态码
     *
     * @param body 响应体对象
     * @param returnType 方法返回类型参数
     * @param selectedContentType 选中的媒体类型
     * @param selectedConverterType 选中的消息转换器类型
     * @param request 服务器请求对象
     * @param response 服务器响应对象
     * @return 处理后的响应体对象
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        // 检查响应体是否为Response类型
        if (!(body instanceof Response<?> r)) {
            return body;
        }

        // 获取响应码
        int code = r.getCode();

        // 如果是成功码或小于100的状态码，直接返回
        if (code == ErrorCode.OK.code || code < 100) {
            return body;
        }

        // 尝试将响应码转换为HTTP状态码并设置到响应中
        HttpStatus status = HttpStatus.resolve(code);
        if (status != null) {
            response.setStatusCode(status);
        }
        return body;
    }
}

