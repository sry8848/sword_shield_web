package com.swordshield.common;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("${server.error.path:${error.path:/error}}")
    public Response<Object> error(HttpServletRequest request, HttpServletResponse response) {
        int status = resolveStatus(request);
        response.setStatus(status);

        ErrorCode code = ErrorCode.fromHttpStatus(status);
        String message = resolveMessage(request, code);
        return Response.error(code, message);
    }

    private int resolveStatus(HttpServletRequest request) {
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

    private String resolveMessage(HttpServletRequest request, ErrorCode code) {
        Object msg = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (msg instanceof String s && !s.isBlank()) {
            return s;
        }
        return code.message;
    }
}

