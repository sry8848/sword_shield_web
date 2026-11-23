package com.swordshield.next.identity.interfaces.http;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.nimbusds.jose.jwk.RSAKey;
import com.swordshield.next.common.ApiEnvelope;
import com.swordshield.next.identity.application.PortalAuthentication;
import com.swordshield.next.identity.application.PortalAuthentication.Profile;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@RestController
class PortalAuthController {

    private static final String PORTAL_PATH = "/api/v1/auth/portal";
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
        if (!origin.equals(requestOrigin)) {
            throw new PortalAuthProblem(HttpStatus.FORBIDDEN, "FORBIDDEN", "禁止访问", "请求来源未获授权");
        }
        JsonNode phone = body.path("phone");
        JsonNode password = body.path("password");
        if (!phone.isString() || !PHONE.matcher(phone.asString()).matches()
                || !password.isString() || password.asString().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        // 2. 使用可信来源地址验密。用例返回时，两条持久化记录都已提交。
        var session = authentication.login(phone.asString(), password.asString(), clientIp(request));

        // 3. 将续期凭证交给浏览器 Cookie，响应体只返回 Access Token 和身份摘要。
        var cookie = ResponseCookie.from("portal_refresh_token", session.refreshToken())
                .httpOnly(true).secure(secureCookie).sameSite("Lax").path(PORTAL_PATH)
                .maxAge(PortalAuthentication.REFRESH_IDLE).build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiEnvelope.ok(new AuthSession(session.accessToken(), "Bearer",
                        PortalAuthentication.ACCESS_SECONDS, session.user()), tracer.currentSpan().context().traceId()));
    }

    /** 从已验签的 Bearer JWT 获取当前用户。 */
    @GetMapping(PORTAL_PATH + "/me")
    ResponseEntity<ApiEnvelope<Profile>> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiEnvelope.ok(authentication.currentProfile(jwt.getSubject()), tracer.currentSpan().context().traceId()));
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

    private record AuthSession(String accessToken, String tokenType, int expiresIn, Profile user) {
    }
}
