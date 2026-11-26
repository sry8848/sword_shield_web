package com.swordshield.next.identity;

import com.swordshield.next.common.web.ServletProblemAdvice;
import com.swordshield.next.identity.application.AdminAuthentication;
import com.swordshield.next.identity.application.PortalAuthentication;
import com.swordshield.next.identity.interfaces.http.PortalAuthProblem;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/** 按认证接口契约处理 Identity 特有的失败。 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class IdentityMvcExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(IdentityMvcExceptionHandler.class);

    private final ServletProblemAdvice problems;

    /** 复用 Servlet MVC 的 Problem Details 字段和请求追踪。 */
    IdentityMvcExceptionHandler(ServletProblemAdvice problems) {
        this.problems = problems;
    }

    /** 将 Portal 登录和刷新用例的拒绝原因映射到接口契约。 */
    @ExceptionHandler(PortalAuthentication.Rejected.class)
    ResponseEntity<ProblemDetail> handlePortalRejection(PortalAuthentication.Rejected failure) {
        // 1. 登录失败原因只用于内部选择响应，三类凭证错误返回相同说明。
        ProblemDetail body = switch (failure.failure()) {
            case INVALID_CREDENTIALS -> problems.problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "未认证", "账号或密码错误");
            case RATE_LIMITED -> problems.problem(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "请求过于频繁", "请求频率超过限制");
            case INVALID_REFRESH -> problems.problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "未认证", "刷新令牌缺失或无效");
            case REFRESH_EXPIRED -> problems.problem(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "未认证", "刷新令牌已过期");
            case REFRESH_REPLAYED -> problems.problem(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REPLAYED", "未认证",
                    "已用刷新令牌被重放，所属会话已撤销，请重新登录。");
        };
        return ResponseEntity.status(body.getStatus()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    /** Portal MVC 与 Bearer 过滤链共用项目的错误字段和同一请求的追踪上下文。 */
    @ExceptionHandler(PortalAuthProblem.class)
    ResponseEntity<ProblemDetail> handlePortalProblem(PortalAuthProblem failure) {
        // 1. 使用接口入口选定的状态和公开说明生成标准响应。
        var body = problems.problem(failure.status(), failure.code(), failure.title(), failure.getMessage());
        return ResponseEntity.status(failure.status()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    /** 将 Admin 认证与会话校验拒绝原因映射为契约规定的状态与错误码。 */
    @ExceptionHandler(AdminAuthentication.Rejected.class)
    ResponseEntity<ProblemDetail> handleAdminRejection(AdminAuthentication.Rejected failure) {
        ProblemDetail body = switch (failure.failure()) {
            case INVALID_CREDENTIALS -> problems.problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "未认证", "账号或密码错误");
            case UNAUTHENTICATED -> problems.problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "未认证", "后台 Session 缺失或无效");
            case ACCESS_REVOKED -> problems.problem(HttpStatus.FORBIDDEN, "ADMIN_ACCESS_REVOKED", "禁止访问", "账号没有有效的后台访问资格。");
            case RATE_LIMITED -> problems.problem(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "请求过于频繁", "请求频率超过限制");
        };
        return ResponseEntity.status(body.getStatus()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    /** Admin 在线认证读取或销毁遇到 Redis/MySQL 故障时返回 503，Portal 保持既有 500 语义。 */
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Object> handleDataAccess(DataAccessException exception, HttpServletRequest servletRequest,
            WebRequest request) {
        String uri = servletRequest.getRequestURI();
        if (uri.startsWith("/api/v1/auth/admin/") || uri.startsWith("/internal/admin/session/")) {
            LOG.warn("Admin 认证存储访问异常: {}", exception.getClass().getSimpleName());
            ProblemDetail body = problems.problem(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_STATE_UNAVAILABLE",
                    "服务不可用", "暂时无法确认后台在线认证状态。");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(body);
        }
        return problems.handleUnexpected(exception, request);
    }
}
