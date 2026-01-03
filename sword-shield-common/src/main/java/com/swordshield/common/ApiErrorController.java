package com.swordshield.common;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API错误控制器，用于处理系统错误并返回统一格式的错误响应
 */
@RestController
public class ApiErrorController implements ErrorController {

    /**
     * 处理错误请求，返回统一格式的错误响应
     *
     * @param request  HTTP请求对象，包含错误相关信息
     * @param response HTTP响应对象，用于设置响应状态码
     * @return Response<Object> 统一格式的错误响应对象
     */
    @RequestMapping("${server.error.path:${error.path:/error}}")
    public Response<Object> error(HttpServletRequest request, HttpServletResponse response) {
        int status = resolveStatus(request);
        response.setStatus(status);

        ErrorCode code = ErrorCode.fromHttpStatus(status);
        String message = resolveMessage(request, code);
        return Response.error(code, message);
    }

    /**
     * 解析请求中的错误状态码
     *
     * @param request HTTP请求对象，从中获取错误状态码属性
     * @return int 解析得到的HTTP状态码，如果解析失败则返回服务器错误码
     */
    private int resolveStatus(HttpServletRequest request) {
        // 从请求属性中获取错误状态码
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status instanceof Integer i) {
            return i;
        }
        if (status instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                return ErrorCode.SERVER_ERROR.code;
            }
        }
        return ErrorCode.SERVER_ERROR.code;
    }

    /**
     * 解析请求中的错误消息
     *
     * @param request HTTP请求对象，从中获取错误消息属性
     * @param code    错误码，当请求中没有有效消息时使用错误码的默认消息
     * @return String 解析得到的错误消息，如果解析失败则返回错误码的默认消息
     */
    private String resolveMessage(HttpServletRequest request, ErrorCode code) {
        // 从请求属性中获取错误消息
        Object msg = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (msg instanceof String s && !s.isBlank()) {
            return s;
        }
        return code.message;
    }
}

