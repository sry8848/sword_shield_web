package com.swordshield.next.gateway;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpHeaders;

final class AuthCookies {

    static final String ADMIN = "admin_session";
    static final String PORTAL = "portal_refresh_token";

    private AuthCookies() {
    }

    /** 删除指定的会话 Cookie，保留同一请求中属于当前应用的其他 Cookie。 */
    static void remove(HttpHeaders headers, String... names) {
        List<String> cookies = headers.getOrEmpty(HttpHeaders.COOKIE).stream()
                .flatMap(header -> Arrays.stream(header.split(";")))
                .map(String::trim)
                .filter(cookie -> Arrays.stream(names).noneMatch(name -> cookie.startsWith(name + "=")))
                .toList();
        if (cookies.isEmpty()) {
            headers.remove(HttpHeaders.COOKIE);
        } else {
            headers.set(HttpHeaders.COOKIE, String.join("; ", cookies));
        }
    }
}
