package com.swordshield.next.identity.interfaces.http;

import org.springframework.http.HttpStatus;

/** Portal 认证入口需要直接映射到接口契约的失败。 */
public final class PortalAuthProblem extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String title;

    PortalAuthProblem(HttpStatus status, String code, String title, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }
}
