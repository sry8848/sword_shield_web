package com.swordshield.next.identity.interfaces.http;

import java.util.List;
import java.util.regex.Pattern;

import com.swordshield.next.common.ApiEnvelope;
import com.swordshield.next.identity.application.AdminAuthentication;
import com.swordshield.next.identity.application.AdminAuthentication.AdminIdentity;
import com.swordshield.next.identity.application.AdminAuthentication.Failure;
import com.swordshield.next.identity.application.AdminAuthentication.Rejected;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@RestController
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class AdminAuthController {

    private static final String ADMIN_PATH = "/api/v1/auth/admin";
    private static final Pattern PHONE = Pattern.compile("1[3-9][0-9]{9}");

    private final AdminAuthentication authentication;
    private final Tracer tracer;
    private final HttpSessionCsrfTokenRepository csrfTokenRepository;
    private final DefaultCookieSerializer cookieSerializer;
    private final CookieHttpSessionIdResolver sessionIdResolver;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final String trustedGatewayIp;

    AdminAuthController(AdminAuthentication authentication, Tracer tracer,
            HttpSessionCsrfTokenRepository csrfTokenRepository,
            DefaultCookieSerializer cookieSerializer,
            CookieHttpSessionIdResolver sessionIdResolver,
            @Value("${identity.admin.trusted-gateway-ip:127.0.0.1}") String trustedGatewayIp) {
        this.authentication = authentication;
        this.tracer = tracer;
        this.csrfTokenRepository = csrfTokenRepository;
        this.cookieSerializer = cookieSerializer;
        this.sessionIdResolver = sessionIdResolver;
        this.sessionStrategy = new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfTokenRepository)));
        this.trustedGatewayIp = trustedGatewayIp;
    }

    /** 获取当前 Session 关联的 CSRF token，必要时建立未认证的临时 Session。 */
    @GetMapping(ADMIN_PATH + "/csrf")
    ResponseEntity<ApiEnvelope<CsrfData>> csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokenRepository.loadToken(request);
        if (token == null) {
            token = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(token, request, response);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiEnvelope.ok(new CsrfData(token.getToken()), traceId()));
    }

    /** 验证手机号、密码与固定后台角色，更换 Session ID 并持久化 Redis 后返回当前后台身份。 */
    @PostMapping(path = ADMIN_PATH + "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiEnvelope<AdminIdentity>> login(@RequestBody JsonNode body,
            HttpServletRequest request, HttpServletResponse response) {
        JsonNode phone = body.path("phone");
        JsonNode password = body.path("password");
        if (!phone.isString() || !PHONE.matcher(phone.asString()).matches()
                || !password.isString() || password.asString().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        var authenticated = authentication.login(phone.asString(), password.asString(), clientIp(request));
        var authorities = authenticated.identity().roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        var authToken = UsernamePasswordAuthenticationToken.authenticated(
                authenticated.identity().user().id(), null, authorities);

        // 确保已有临时 Session 时更换 Session ID 并轮换 CSRF，无 Session 时建立新 Session。
        request.getSession(true);
        sessionStrategy.onAuthentication(authToken, request, response);

        HttpSession session = request.getSession(false);
        session.setAttribute(AdminSessionConfiguration.ACCOUNT_ID_ATTR, authenticated.identity().user().id());
        session.setAttribute(AdminSessionConfiguration.CREDENTIAL_VERSION_ATTR, authenticated.credentialVersion());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiEnvelope.ok(authenticated.identity(), traceId()));
    }

    /** 查询当前后台身份，每次从主库实时核对启用状态、凭证版本与有效角色。 */
    @GetMapping(ADMIN_PATH + "/me")
    ResponseEntity<ApiEnvelope<AdminIdentity>> me(HttpServletRequest request) {
        AdminIdentity identity = verifyActiveSession(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiEnvelope.ok(identity, traceId()));
    }

    /** 销毁当前后台会话并清除本端 Cookie；无 Cookie、已过期或匿名临时 Session 同样幂等返回 200。 */
    @PostMapping(ADMIN_PATH + "/logout")
    ResponseEntity<ApiEnvelope<LogoutData>> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = cookieSerializer.readCookieValues(request).isEmpty()
                ? null : request.getSession(false);
        if (session != null) {
            new SecurityContextLogoutHandler().logout(request, response, null);
        } else {
            sessionIdResolver.expireSession(request, response);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiEnvelope.ok(new LogoutData(true), traceId()));
    }

    /** 供 Gateway ForwardAuth 同步调用的内部会话与实时角色校验入口。 */
    @RequestMapping(path = "/internal/admin/session/validate", method = {
            RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE
    })
    ResponseEntity<ValidatedAdminSession> validateInternal(HttpServletRequest request) {
        AdminIdentity identity = verifyActiveSession(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new ValidatedAdminSession(identity.user().id(), identity.roles()));
    }

    private AdminIdentity verifyActiveSession(HttpServletRequest request) {
        if (cookieSerializer.readCookieValues(request).isEmpty()) {
            throw new Rejected(Failure.UNAUTHENTICATED);
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new Rejected(Failure.UNAUTHENTICATED);
        }
        Object accountId = session.getAttribute(AdminSessionConfiguration.ACCOUNT_ID_ATTR);
        Object credentialVersion = session.getAttribute(AdminSessionConfiguration.CREDENTIAL_VERSION_ATTR);
        if (!(accountId instanceof String id) || !(credentialVersion instanceof Integer version)) {
            throw new Rejected(Failure.UNAUTHENTICATED);
        }
        return authentication.currentIdentity(id, version);
    }

    private String clientIp(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        String gatewayAddress = request.getHeader("X-Identity-Client-IP");
        return peer.equals(trustedGatewayIp) && gatewayAddress != null ? gatewayAddress : peer;
    }

    private String traceId() {
        return tracer.currentSpan().context().traceId();
    }

    record CsrfData(String token) {
    }

    record LogoutData(boolean loggedOut) {
    }

    record ValidatedAdminSession(String accountId, List<String> roles) {
    }
}
