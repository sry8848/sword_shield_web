package com.swordshield.next.identity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "identity.admin.session-namespace=xihu:admin:http-test",
        "spring.data.redis.timeout=1s",
        "APP_VERSION=identity-test"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "ADMIN_AUTH_INTEGRATION", matches = "true")
class AdminAuthHttpTest {

    private static final String AUTH = "/api/v1/auth/admin";
    private static final String VALIDATE = "/internal/admin/session/validate";
    private static final String ORIGIN = "http://localhost:5174";
    private static final String ADMIN = "13800000002";
    private static final String OPERATOR = "13800000004";
    private static final String CHECKIN = "13800000005";
    private static final String ADMIN_PASSWORD = "LocalAdmin-Only!2026";
    private static final String OPERATOR_PASSWORD = "LocalOperator-Only!2026";
    private static final String CHECKIN_PASSWORD = "LocalCheckin-Only!2026";
    private static final String PREFIX = "xihu:admin:http-test:sessions:";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = new JsonMapper();

    @DynamicPropertySource
    static void signingKey(DynamicPropertyRegistry registry) {
        PortalTestSigningKey.configure(registry);
    }

    /** 每个用例独立验证同一固定窗口内的节流行为。 */
    @BeforeEach
    void resetBudgets() {
        redis.delete(redis.keys("identity:admin:login:*"));
    }

    /** 三类账号均可准入。临时会话在登录时轮换，旧 CSRF token 同时失效。 */
    @Test
    void startsRotatedRedisSessionsForAllFixedRoles() throws Exception {
        for (var account : List.of(new String[]{ADMIN, ADMIN_PASSWORD, "SUPER_ADMIN"},
                new String[]{OPERATOR, OPERATOR_PASSWORD, "EVENT_OPERATOR"},
                new String[]{CHECKIN, CHECKIN_PASSWORD, "CHECKIN_STAFF"})) {
            var anonymous = csrf(null);
            var response = login(account[0], account[1], anonymous, "127.0.0.1");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Set-Cookie").orElseThrow())
                    .contains("Path=/api/v1", "HttpOnly", "SameSite=Lax")
                    .doesNotContain("Max-Age", "Domain=");
            String session = cookie(response);
            assertThat(session).isNotEqualTo(anonymous.cookie());
            assertThat(redis.hasKey(sessionKey(anonymous.cookie()))).isFalse();
            assertThat(redis.getExpire(sessionKey(session))).isBetween(28790L, 28800L);
            assertThat(body(response).at("/data/roles/0").asString()).isEqualTo(account[2]);
            assertThat(get(AUTH + "/me", session).statusCode()).isEqualTo(200);
            assertThat(body(get(VALIDATE, session)).path("accountId").asString())
                    .isEqualTo(body(response).at("/data/user/id").asString());
            assertProblem(send("POST", VALIDATE, session, anonymous.token(), ORIGIN, null, null),
                    403, "FORBIDDEN");
        }
    }

    /** 普通账号被拒绝准入，其他错误凭证使用相同的公开错误。 */
    @Test
    void rejectsOrdinaryAccountsAndInvalidCredentials() throws Exception {
        var anonymous = csrf(null);
        assertProblem(login("13800000001", "LocalPortal-Only!2026", anonymous, "127.0.0.1"),
                403, "ADMIN_ACCESS_REVOKED");
        var wrong = login(ADMIN, "wrong", anonymous, "127.0.0.1");
        var unknown = login("13999990000", "wrong", anonymous, "127.0.0.1");
        assertProblem(wrong, 401, "UNAUTHENTICATED");
        assertProblem(unknown, 401, "UNAUTHENTICATED");
        assertThat(body(wrong).path("detail")).isEqualTo(body(unknown).path("detail"));
        jdbc.update("UPDATE identity_account SET status='DISABLED' WHERE phone=?", ADMIN);
        try {
            var disabled = login(ADMIN, ADMIN_PASSWORD, anonymous, "127.0.0.1");
            assertProblem(disabled, 401, "UNAUTHENTICATED");
            assertThat(body(disabled).path("detail")).isEqualTo(body(wrong).path("detail"));
        } finally {
            jdbc.update("UPDATE identity_account SET status='ENABLED' WHERE phone=?", ADMIN);
        }
    }

    /** 分别用多来源重复手机号及同来源多手机号触发两个独立窗口。 */
    @Test
    void limitsPhoneAndIpIndependently() throws Exception {
        var anonymous = csrf(null);
        for (int attempt = 0; attempt < 5; attempt++) {
            assertProblem(login("13999990000", "wrong", anonymous, "192.0.2." + (attempt + 1)),
                    401, "UNAUTHENTICATED");
        }
        assertProblem(login("13999990000", "wrong", anonymous, "192.0.2.100"), 429, "TOO_MANY_REQUESTS");
        for (int attempt = 0; attempt < 30; attempt++) {
            assertProblem(login("13999990%03d".formatted(attempt + 1), "wrong", anonymous, "192.0.2.200"),
                    401, "UNAUTHENTICATED");
        }
        assertProblem(login("13999990031", "wrong", anonymous, "192.0.2.200"), 429, "TOO_MANY_REQUESTS");
    }

    /** 后台写入和有效会话退出需要 Origin 和 CSRF。其余退出请求以幂等方式清理 Cookie。 */
    @Test
    void checksOriginAndCsrfAndClearsSessionsOnLogout() throws Exception {
        var anonymous = csrf(null);
        String session = cookie(login(ADMIN, ADMIN_PASSWORD, csrf(null), "127.0.0.1"));
        var authenticated = csrf(session);
        assertProblem(send("POST", VALIDATE, session, authenticated.token(), "http://localhost:9999", null, null),
                403, "FORBIDDEN");
        assertProblem(send("POST", VALIDATE, session, null, ORIGIN, null, null), 403, "FORBIDDEN");
        assertThat(send("POST", VALIDATE, session, authenticated.token(), ORIGIN, null, null).statusCode())
                .isEqualTo(200);
        assertProblem(send("POST", AUTH + "/logout", session, null, ORIGIN, null, null), 403, "FORBIDDEN");
        assertProblem(send("POST", AUTH + "/logout", session, authenticated.token(), null, null, null),
                403, "FORBIDDEN");
        assertLoggedOut(send("POST", AUTH + "/logout", session, authenticated.token(), ORIGIN, null, null));
        assertThat(redis.hasKey(sessionKey(session))).isFalse();
        assertProblem(get(AUTH + "/me", session), 401, "UNAUTHENTICATED");
        assertLoggedOut(send("POST", AUTH + "/logout", session, null, ORIGIN, null, null));
        assertLoggedOut(send("POST", AUTH + "/logout", anonymous.cookie(), null, ORIGIN, null, null));
        assertLoggedOut(send("POST", AUTH + "/logout", null, null, ORIGIN, null, null));
    }

    /** 角色撤销与凭证版本按数据库当前状态生效，已有 Session 无需重建。 */
    @Test
    void checksCurrentRolesAndCredentialVersion() throws Exception {
        var loggedIn = login(OPERATOR, OPERATOR_PASSWORD, csrf(null), "127.0.0.1");
        String session = cookie(loggedIn);
        String account = body(loggedIn).at("/data/user/id").asString();
        String role = jdbc.queryForObject("SELECT id FROM identity_role WHERE code='EVENT_OPERATOR'", String.class);
        jdbc.update("DELETE FROM identity_account_role WHERE account_id=? AND role_id=?", account, role);
        try {
            assertProblem(get(AUTH + "/me", session), 403, "ADMIN_ACCESS_REVOKED");
            assertProblem(get(VALIDATE, session), 403, "ADMIN_ACCESS_REVOKED");
        } finally {
            jdbc.update("INSERT INTO identity_account_role (account_id, role_id) VALUES (?,?)", account, role);
        }
        jdbc.update("UPDATE identity_account SET status='DISABLED', credential_version=credential_version+1 WHERE id=?",
                account);
        try {
            assertProblem(get(AUTH + "/me", session), 401, "UNAUTHENTICATED");
            assertProblem(get(VALIDATE, session), 401, "UNAUTHENTICATED");
        } finally {
            jdbc.update("UPDATE identity_account SET status='ENABLED' WHERE id=?", account);
        }
        assertProblem(get(AUTH + "/me", session), 401, "UNAUTHENTICATED");
    }

    /** Portal 处理请求时不读写已存在的后台会话。 */
    @Test
    void portalDoesNotTouchAdminSessions() throws Exception {
        String session = cookie(login(ADMIN, ADMIN_PASSWORD, csrf(null), "127.0.0.1"));
        redis.expire(sessionKey(session), Duration.ofMinutes(2));
        var portal = get("/api/v1/auth/jwks", session);
        assertThat(portal.statusCode()).isEqualTo(200);
        assertThat(portal.headers().allValues("Set-Cookie")).isEmpty();
        assertThat(redis.getExpire(sessionKey(session))).isBetween(110L, 120L);
    }

    /** Redis 实际停止响应时，退出返回可重试错误并保留浏览器 Cookie。 */
    @Test
    void retainsCookieWhenRedisCannotConfirmLogout() throws Exception {
        String session = cookie(login(ADMIN, ADMIN_PASSWORD, csrf(null), "127.0.0.1"));
        var credentials = csrf(session);
        redis.execute((RedisCallback<Object>) connection ->
                connection.execute("CLIENT", "PAUSE".getBytes(StandardCharsets.US_ASCII),
                        "3500".getBytes(StandardCharsets.US_ASCII), "ALL".getBytes(StandardCharsets.US_ASCII)));
        var response = send("POST", AUTH + "/logout", session, credentials.token(), ORIGIN, null, null);
        assertProblem(response, 503, "AUTH_STATE_UNAVAILABLE");
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        Thread.sleep(3500);
    }

    private Session csrf(String existing) throws Exception {
        var response = get(AUTH + "/csrf", existing);
        assertThat(response.statusCode()).isEqualTo(200);
        return new Session(existing == null ? cookie(response) : existing, body(response).at("/data/token").asString());
    }

    private HttpResponse<String> login(String phone, String password, Session session, String ip) throws Exception {
        return send("POST", AUTH + "/login", session.cookie(), session.token(), ORIGIN, ip,
                json.writeValueAsString(Map.of("phone", phone, "password", password)));
    }

    private HttpResponse<String> get(String path, String cookie) throws Exception {
        return send("GET", path, cookie, null, null, null, null);
    }

    private HttpResponse<String> send(String method, String path, String cookie, String token,
            String origin, String ip, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10));
        if (cookie != null) request.header("Cookie", cookie);
        if (token != null) request.header("X-CSRF-TOKEN", token);
        if (origin != null) request.header("Origin", origin);
        if (ip != null) request.header("X-Identity-Client-IP", ip);
        if (body != null) request.header("Content-Type", "application/json");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertProblem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(body(response).path("code").asString()).isEqualTo(code);
    }

    private void assertLoggedOut(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().allValues("Set-Cookie"))
                .anySatisfy(value -> assertThat(value).startsWith("admin_session=").contains("Max-Age=0", "Path=/api/v1"));
        assertThat(body(response).at("/data/loggedOut").asBoolean()).isTrue();
    }

    private JsonNode body(HttpResponse<String> response) {
        return json.readTree(response.body());
    }

    private static String cookie(HttpResponse<String> response) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(value -> value.startsWith("admin_session="))
                .findFirst().orElseThrow().split(";", 2)[0];
    }

    private static String sessionKey(String cookie) {
        return PREFIX + cookie.substring("admin_session=".length());
    }

    private record Session(String cookie, String token) {}
}
