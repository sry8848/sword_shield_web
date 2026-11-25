package com.swordshield.next.identity.interfaces.http;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.nimbusds.jose.jwk.RSAKey;
import com.swordshield.next.common.ApiEnvelope;
import com.swordshield.next.identity.application.PortalAuthentication;
import com.swordshield.next.identity.application.PortalAuthentication.Profile;
import com.swordshield.next.identity.application.PortalAuthentication.SessionTokens;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@RestController
class PortalAuthController {

    private static final Logger LOG = LoggerFactory.getLogger(PortalAuthController.class);

    private static final String PORTAL_PATH = "/api/v1/auth/portal";
    private static final String REFRESH_COOKIE = "portal_refresh_token";
    private static final Pattern REFRESH_TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final Pattern PHONE = Pattern.compile("1[3-9][0-9]{9}");

    private final PortalAuthentication authentication;
    private final Tracer tracer;
    private final RSAKey key;
    private final String origin;
    private final String trustedGatewayIp;
    private final boolean secureCookie;

    /** 装配认证用例、JWKS 公钥来源及当前部署的安全边界。 */
    PortalAuthController(PortalAuthentication authentication, Tracer tracer, RSAKey key,
            @Value("${identity.portal.origin}") String origin,
            @Value("${identity.portal.trusted-gateway-ip}") String trustedGatewayIp,
            @Value("${identity.portal.secure-cookie:false}") boolean secureCookie) {
        this.authentication = authentication;
        this.tracer = tracer;
        this.key = key;
        this.origin = origin;
        this.trustedGatewayIp = trustedGatewayIp;
        this.secureCookie = secureCookie;
    }

    /** 校验浏览器来源和登录字段，返回 Access Token 和专用 Refresh Cookie。 */
    @PostMapping(path = PORTAL_PATH + "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiEnvelope<AuthSession>> login(@RequestBody JsonNode body,
            @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String requestOrigin,
            HttpServletRequest request) {
        // 1. 来源和格式不合法的请求不消耗限流预算。
        requireTrustedOrigin(requestOrigin);
        JsonNode phone = body.path("phone");
        JsonNode password = body.path("password");
        if (!phone.isString() || !PHONE.matcher(phone.asString()).matches()
                || !password.isString() || password.asString().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        // 2. 使用可信来源地址验密。用例返回时，两条持久化记录都已提交。
        var session = authentication.login(phone.asString(), password.asString(), clientIp(request));

        return sessionResponse(session);
    }

    /** 根据 Portal 刷新 Cookie 签发新的 Access Token，首次轮换同时写入后继 Cookie。 */
    @PostMapping(PORTAL_PATH + "/refresh")
    ResponseEntity<ApiEnvelope<AuthSession>> refresh(
            @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String requestOrigin,
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        // 1. 先校验来源与 Cookie 格式，再解析会话。
        requireTrustedOrigin(requestOrigin);
        if (!validRefreshToken(refreshToken)) {
            throw new PortalAuthProblem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "未认证", "刷新令牌缺失或无效");
        }
        return sessionResponse(authentication.refresh(refreshToken, traceId()));
    }

    /** 撤销当前 Portal 会话，确认退出成功后清除 Cookie。 */
    @PostMapping(PORTAL_PATH + "/logout")
    ResponseEntity<ApiEnvelope<LogoutData>> logout(
            @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String requestOrigin,
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        // 1. 只允许可信来源退出，有凭证时确认会话状态。
        requireTrustedOrigin(requestOrigin);
        if (validRefreshToken(refreshToken)) {
            try {
                authentication.logout(refreshToken);
            } catch (DataAccessException | TransactionException failure) {
                // 存储失败时留下 Cookie，以便浏览器下次继续退出。
                LOG.warn("Portal logout state could not be confirmed: {}", failure.getClass().getSimpleName());
                throw new PortalAuthProblem(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_STATE_UNAVAILABLE",
                        "服务不可用", "暂时无法确认退出结果");
            }
        }

        // 2. 用签发时的同名、同路径 Cookie 执行清除。
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .body(ApiEnvelope.ok(new LogoutData(true), traceId()));
    }

    /** 写入共用的凭证响应，仅在用例交付 Refresh Token 时设置 Cookie。 */
    private ResponseEntity<ApiEnvelope<AuthSession>> sessionResponse(SessionTokens session) {
        var response = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if (session.refreshToken() != null) {
            response.header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(), session.cookieMaxAge()).toString());
        }
        return response.body(ApiEnvelope.ok(new AuthSession(session.accessToken(), "Bearer",
                PortalAuthentication.ACCESS_SECONDS, session.user()), traceId()));
    }

    /** 登录、刷新、清除共用同一 Portal Cookie 属性。 */
    private ResponseCookie refreshCookie(String refreshToken, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true).secure(secureCookie).sameSite("Lax").path(PORTAL_PATH)
                .maxAge(maxAge).build();
    }

    /** 仅接受 Portal 签发的 256 位 Base64URL 凭证格式。 */
    private static boolean validRefreshToken(String refreshToken) {
        return refreshToken != null && REFRESH_TOKEN.matcher(refreshToken).matches();
    }

    /** 对所有可写入 Portal Cookie 的请求使用同一来源校验。 */
    private void requireTrustedOrigin(String requestOrigin) {
        if (!origin.equals(requestOrigin)) {
            throw new PortalAuthProblem(HttpStatus.FORBIDDEN, "FORBIDDEN", "禁止访问", "请求来源未获授权");
        }
    }

    /** 从已验签的 Bearer JWT 获取当前用户。 */
    @GetMapping(PORTAL_PATH + "/me")
    ResponseEntity<ApiEnvelope<Profile>> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiEnvelope.ok(authentication.currentProfile(jwt.getSubject()), traceId()));
    }

    /** 发布验签所需公钥，私钥参数不会进入 JWKS。 */
    @GetMapping("/api/v1/auth/jwks")
    Map<String, List<Map<String, Object>>> jwks() {
        return Map.of("keys", List.of(key.toPublicJWK().toJSONObject()));
    }

    /** 只采纳可信 Gateway 注入的客户端地址，直连时使用 TCP 对端。 */
    private String clientIp(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        String gatewayAddress = request.getHeader("X-Identity-Client-IP");
        return peer.equals(trustedGatewayIp) && gatewayAddress != null ? gatewayAddress : peer;
    }

    /** 取当前请求的追踪标识，用于响应和重放审计。 */
    private String traceId() {
        return tracer.currentSpan().context().traceId();
    }

    private record LogoutData(boolean loggedOut) {
    }

    private record AuthSession(String accessToken, String tokenType, int expiresIn, Profile user) {
    }
}
