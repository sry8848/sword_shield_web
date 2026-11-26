package com.swordshield.next.identity;

import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.swordshield.next.common.UuidV7Ids;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "PORTAL_AUTH_INTEGRATION", matches = "true")
class PortalAuthHttpTest {

    private static final String AUTH = "/api/v1/auth/portal";
    private static final String ORIGIN = "http://localhost:5173";
    private static final String ISSUER = "http://localhost:8080";
    private static final String PHONE = "13800000001";
    private static final String PASSWORD = "LocalPortal-Only!2026";
    private static final String REDIS_PREFIX = "identity:portal:login:";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private JwtEncoder encoder;

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = new JsonMapper();

    @DynamicPropertySource
    static void signingKey(DynamicPropertyRegistry registry) {
        PortalTestSigningKey.configure(registry);
    }

    /** 核对登录、Cookie、数据库记录、公开 JWKS 和真实 Bearer 校验。 */
    @Test
    void createsSessionAndReturnsVerifiedCurrentIdentity() throws Exception {
        // 1. 登录后检查凭证形状和浏览器 Cookie 属性。
        redis.delete(List.of(REDIS_PREFIX + "phone:" + PHONE, REDIS_PREFIX + "ip:192.0.2.80"));
        var login = login(PHONE, PASSWORD, "192.0.2.80");
        assertThat(login.statusCode()).isEqualTo(200);
        var session = json.readTree(login.body()).path("data");
        assertThat(session.path("tokenType").asString()).isEqualTo("Bearer");
        assertThat(session.path("expiresIn").asInt()).isEqualTo(600);
        String cookie = login.headers().firstValue("Set-Cookie").orElseThrow();
        assertThat(cookie).startsWith("portal_refresh_token=").contains("HttpOnly", "SameSite=Lax",
                "Path=/api/v1/auth/portal", "Max-Age=604800").doesNotContain("Domain=");
        String refresh = cookie.substring("portal_refresh_token=".length(), cookie.indexOf(';'));
        assertThat(refresh).hasSize(43);
        assertThat(login.body()).doesNotContain(refresh);

        // 2. 用公开的 JWKS 验证签名、期限和数据库中的首个凭证哈希。
        SignedJWT token = SignedJWT.parse(session.path("accessToken").asString());
        var jwks = get("/api/v1/auth/jwks", null);
        assertThat(jwks.statusCode()).isEqualTo(200);
        var key = (RSAKey) JWKSet.parse(jwks.body()).getKeyByKeyId(token.getHeader().getKeyID());
        assertThat(key.isPrivate()).isFalse();
        assertThat(token.verify(new RSASSAVerifier(key))).isTrue();
        var claims = token.getJWTClaimsSet();
        assertThat(claims.getExpirationTime().toInstant()).isEqualTo(claims.getIssueTime().toInstant().plusSeconds(600));
        assertThat(claims.getAudience()).containsExactly("portal-api");
        String sessionId = claims.getStringClaim("sid");
        assertThat(sessionId).hasSize(36);
        UUID persistedSessionId = UUID.fromString(sessionId);
        assertThat(persistedSessionId.toString()).isEqualTo(sessionId);
        assertThat(persistedSessionId.version()).isEqualTo(7);
        assertThat(persistedSessionId.variant()).isEqualTo(2);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(refresh.getBytes(StandardCharsets.US_ASCII));
        assertThat(jdbc.queryForObject("SELECT token_hash FROM identity_refresh_token WHERE session_id = ?",
                byte[].class, sessionId)).isEqualTo(digest);
        assertThat(jdbc.queryForObject("SELECT client_type FROM identity_session WHERE id = ?",
                String.class, sessionId)).isEqualTo("PORTAL");

        // 3. 只有有效 Portal JWT 可访问当前身份，且只返回脱敏账号摘要。
        var me = get(AUTH + "/me", token.serialize());
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(json.readTree(me.body()).path("data")).isEqualTo(session.path("user"));
        assertThat(session.path("user").path("maskedPhone").asString()).isEqualTo("138****0001");
        assertThat(session.path("user").path("id").isString()).isTrue();
        assertThat(session.path("user").path("id").asString())
                .isEqualTo(claims.getSubject()).isEqualTo("01935f3a-7b2c-7abc-8def-000000000011");
        assertProblem(get(AUTH + "/me", null), 401, "UNAUTHENTICATED");
        assertProblem(get(AUTH + "/me", "invalid"), 401, "UNAUTHENTICATED");
        assertProblem(get(AUTH + "/me", fixtureToken(key, "admin-api", Instant.now().plusSeconds(60))),
                401, "UNAUTHENTICATED");
        assertProblem(get(AUTH + "/me", fixtureToken(key, "portal-api", Instant.now().minusSeconds(60))),
                401, "ACCESS_TOKEN_EXPIRED");
    }

    /** 验证错误凭证、字段类型和请求来源均不会产生新会话。 */
    @Test
    void rejectsUnknownWrongDisabledAndInvalidRequests() throws Exception {
        String disabled = "13990000001";
        String unknown = "13990000002";
        String ip = "192.0.2.81";
        redis.delete(List.of(REDIS_PREFIX + "phone:" + PHONE, REDIS_PREFIX + "phone:" + disabled,
                REDIS_PREFIX + "phone:" + unknown, REDIS_PREFIX + "ip:" + ip));
        jdbc.update("INSERT INTO identity_account (id, phone, password_hash, status) "
                + "SELECT ?, ?, password_hash, 'DISABLED' FROM identity_account WHERE phone = ?",
                UuidV7Ids.next(), disabled, PHONE);
        try {
            // 1. 三类凭证失败得到完全相同的公开语义。
            for (var response : List.of(login(PHONE, "wrong-password", ip), login(unknown, PASSWORD, ip),
                    login(disabled, PASSWORD, ip))) {
                assertThat(assertProblem(response, 401, "UNAUTHENTICATED").path("detail").asString())
                        .isEqualTo("账号或密码错误");
                assertThat(response.headers().firstValue("Set-Cookie")).isEmpty();
            }

            // 2. 入口拒绝数字手机号、空密码和未获授权的来源。
            assertProblem(post("{\"phone\":13800000001,\"password\":\"x\"}", ORIGIN, ip), 400, "INVALID_ARGUMENT");
            assertProblem(post(json.writeValueAsString(Map.of("phone", PHONE, "password", "")), ORIGIN, ip),
                    400, "INVALID_ARGUMENT");
            assertProblem(post("{}", "https://other.example", ip), 403, "FORBIDDEN");
            assertThat(redis.opsForValue().get(REDIS_PREFIX + "ip:" + ip)).isEqualTo("3");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM identity_session s JOIN identity_account a "
                    + "ON a.id = s.account_id WHERE a.phone = ?", Integer.class, disabled)).isZero();
        } finally {
            jdbc.update("DELETE FROM identity_account WHERE phone = ?", disabled);
        }
    }

    /** 用真实 Redis 验证手机号和来源 IP 各自的阈值。 */
    @Test
    void appliesIndependentPhoneAndIpWindows() throws Exception {
        // 1. 更换来源 IP 后，第 6 次尝试仍受手机号限额约束。
        String phone = "13990000003";
        redis.delete(REDIS_PREFIX + "phone:" + phone);
        String credentials = json.writeValueAsString(Map.of("phone", phone, "password", PASSWORD));
        var attempts = IntStream.range(90, 96)
                .mapToObj(suffix -> client.sendAsync(loginRequest(credentials, ORIGIN, "192.0.2." + suffix),
                        HttpResponse.BodyHandlers.ofString()))
                .toList();
        int blocked = 0;
        for (var attempt : attempts) {
            var response = attempt.join();
            if (response.statusCode() == 429) {
                assertProblem(response, 429, "TOO_MANY_REQUESTS");
                blocked++;
            } else {
                assertProblem(response, 401, "UNAUTHENTICATED");
            }
        }
        assertThat(blocked).isEqualTo(1);
        assertThat(redis.getExpire(REDIS_PREFIX + "phone:" + phone)).isBetween(1L, 60L);

        // 2. 每次更换手机号，仍受同一来源 IP 的第 31 次阈值约束。
        String ip = "192.0.2.96";
        redis.delete(REDIS_PREFIX + "ip:" + ip);
        for (int attempt = 0; attempt < 30; attempt++) {
            assertProblem(login("13790000" + String.format("%03d", attempt), PASSWORD, ip),
                    401, "UNAUTHENTICATED");
        }
        assertProblem(login("13790000030", PASSWORD, ip), 429, "TOO_MANY_REQUESTS");
    }

    @Test
    void rotatesOnceForConcurrentRequestsAndRevokesOnlyTheReplayedSession() throws Exception {
        var original = sessionLogin("192.0.2.101");
        var anotherDevice = sessionLogin("192.0.2.102");
        String oldCookie = refreshCookie(original);
        String sessionId = accessClaims(original).getStringClaim("sid");

        var request = cookieRequest("/refresh", oldCookie, ORIGIN);
        var first = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        var second = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        var responses = List.of(first.join(), second.join());
        assertThat(responses).allSatisfy(response -> {
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(json.readTree(response.body()).path("data").path("expiresIn").asInt()).isEqualTo(600);
        });
        assertThat(accessClaims(responses.get(0)).getJWTID()).isNotEqualTo(accessClaims(responses.get(1)).getJWTID());
        var rotated = responses.stream().filter(response -> response.headers().firstValue("Set-Cookie").isPresent()).toList();
        assertThat(rotated).hasSize(1);
        String newCookie = refreshCookie(rotated.get(0));
        assertThat(newCookie).isNotEqualTo(oldCookie);
        assertThat(tokenCount(sessionId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT idle_expires_at = DATE_ADD(last_used_at, INTERVAL 7 DAY)
                   AND absolute_expires_at = DATE_ADD(created_at, INTERVAL 30 DAY)
                   AND idle_expires_at > DATE_ADD(created_at, INTERVAL 7 DAY)
                FROM identity_session WHERE id = ?
                """, Boolean.class, sessionId)).isTrue();

        var deadlines = jdbc.queryForMap(
                "SELECT idle_expires_at, absolute_expires_at FROM identity_session WHERE id = ?", sessionId);
        LocalDateTime consumedAt = jdbc.queryForObject("SELECT consumed_at FROM identity_refresh_token WHERE token_hash = ?",
                LocalDateTime.class, cookieHash(oldCookie));
        var duplicate = cookiePost("/refresh", oldCookie, ORIGIN);
        assertThat(duplicate.statusCode()).isEqualTo(200);
        assertThat(duplicate.headers().firstValue("Set-Cookie")).isEmpty();
        assertThat(jdbc.queryForMap("SELECT idle_expires_at, absolute_expires_at FROM identity_session WHERE id = ?",
                sessionId)).isEqualTo(deadlines);
        assertThat(jdbc.queryForObject("SELECT consumed_at FROM identity_refresh_token WHERE token_hash = ?",
                LocalDateTime.class, cookieHash(oldCookie))).isEqualTo(consumedAt);

        jdbc.update("UPDATE identity_refresh_token SET consumed_at = UTC_TIMESTAMP(3) - INTERVAL 11 SECOND WHERE token_hash = ?",
                cookieHash(oldCookie));
        jdbc.update("""
                INSERT INTO identity_portal_refresh_replay_audit (session_id, account_id, trace_id, occurred_at)
                VALUES (?, ?, ?, UTC_TIMESTAMP(3))
                """, sessionId, accessClaims(original).getSubject(), "0".repeat(32));
        try {
            var auditFailure = cookiePost("/refresh", oldCookie, ORIGIN);
            assertProblem(auditFailure, 500, "INTERNAL_ERROR");
            assertThat(auditFailure.headers().firstValue("Set-Cookie")).isEmpty();
            assertThat(jdbc.queryForObject("SELECT revoked_at FROM identity_session WHERE id = ?",
                    LocalDateTime.class, sessionId)).isNull();
        } finally {
            jdbc.update("DELETE FROM identity_portal_refresh_replay_audit WHERE session_id = ?", sessionId);
        }
        var replay = cookiePost("/refresh", oldCookie, ORIGIN);
        var problem = assertProblem(replay, 401, "REFRESH_TOKEN_REPLAYED");
        assertThat(replay.headers().firstValue("Set-Cookie")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT trace_id FROM identity_portal_refresh_replay_audit WHERE session_id = ?",
                String.class, sessionId)).isEqualTo(problem.path("traceId").asString());
        assertProblem(cookiePost("/refresh", newCookie, ORIGIN), 401, "UNAUTHENTICATED");
        assertThat(cookiePost("/refresh", refreshCookie(anotherDevice), ORIGIN).statusCode()).isEqualTo(200);
    }

    @Test
    void rejectsExpiredDisabledAndOldVersionSessions() throws Exception {
        var idle = sessionLogin("192.0.2.103");
        var absolute = sessionLogin("192.0.2.104");
        String phone = "13990000004";
        redis.delete(List.of(REDIS_PREFIX + "phone:" + phone, REDIS_PREFIX + "ip:192.0.2.105",
                REDIS_PREFIX + "ip:192.0.2.106", REDIS_PREFIX + "ip:192.0.2.107"));
        jdbc.update("INSERT INTO identity_account (id, phone, password_hash, status) "
                + "SELECT ?, ?, password_hash, 'ENABLED' FROM identity_account WHERE phone = ?",
                UuidV7Ids.next(), phone, PHONE);
        try {
            var beforeDisable = login(phone, PASSWORD, "192.0.2.105");
            var disabledDevice = login(phone, PASSWORD, "192.0.2.106");
            String accountId = accessClaims(beforeDisable).getSubject();
            var rotated = cookiePost("/refresh", refreshCookie(beforeDisable), ORIGIN);
            String rotatedCookie = refreshCookie(rotated);
            jdbc.update("UPDATE identity_account SET status = 'DISABLED', credential_version = credential_version + 1 WHERE id = ?",
                    accountId);

            assertProblem(login(phone, PASSWORD, "192.0.2.107"), 401, "UNAUTHENTICATED");
            assertProblem(cookiePost("/refresh", refreshCookie(beforeDisable), ORIGIN), 401, "UNAUTHENTICATED");
            assertProblem(cookiePost("/refresh", rotatedCookie, ORIGIN), 401, "UNAUTHENTICATED");
            assertProblem(cookiePost("/refresh", refreshCookie(disabledDevice), ORIGIN), 401, "UNAUTHENTICATED");
            assertThat(get(AUTH + "/me", accessToken(beforeDisable)).statusCode()).isEqualTo(200);
            jdbc.update("UPDATE identity_account SET status = 'ENABLED', credential_version = credential_version + 1 WHERE id = ?",
                    accountId);
            assertProblem(cookiePost("/refresh", rotatedCookie, ORIGIN), 401, "UNAUTHENTICATED");
            assertThat(login(phone, PASSWORD, "192.0.2.107").statusCode()).isEqualTo(200);
        } finally {
            jdbc.update("""
                    DELETE t FROM identity_refresh_token t JOIN identity_session s ON s.id = t.session_id
                    JOIN identity_account a ON a.id = s.account_id WHERE a.phone = ?
                    """, phone);
            jdbc.update("DELETE s FROM identity_session s JOIN identity_account a ON a.id = s.account_id WHERE a.phone = ?", phone);
            jdbc.update("DELETE FROM identity_account WHERE phone = ?", phone);
        }

        jdbc.update("UPDATE identity_session SET idle_expires_at = UTC_TIMESTAMP(3) WHERE id = ?",
                accessClaims(idle).getStringClaim("sid"));
        assertProblem(cookiePost("/refresh", refreshCookie(idle), ORIGIN), 401, "REFRESH_TOKEN_EXPIRED");
        String cappedSession = accessClaims(absolute).getStringClaim("sid");
        jdbc.update("UPDATE identity_session SET absolute_expires_at = UTC_TIMESTAMP(3) + INTERVAL 1 DAY WHERE id = ?",
                cappedSession);
        var capped = cookiePost("/refresh", refreshCookie(absolute), ORIGIN);
        assertThat(capped.statusCode()).isEqualTo(200);
        var maxAge = HttpCookie.parse(capped.headers().firstValue("Set-Cookie").orElseThrow()).get(0).getMaxAge();
        assertThat(maxAge).isPositive().isLessThanOrEqualTo(86_400);
        assertThat(jdbc.queryForObject("""
                SELECT idle_expires_at = absolute_expires_at FROM identity_session WHERE id = ?
                """, Boolean.class, cappedSession)).isTrue();
        jdbc.update("UPDATE identity_session SET absolute_expires_at = UTC_TIMESTAMP(3) WHERE id = ?", cappedSession);
        assertProblem(cookiePost("/refresh", refreshCookie(capped), ORIGIN), 401, "REFRESH_TOKEN_EXPIRED");
        assertLoggedOut(cookiePost("/logout", refreshCookie(capped), ORIGIN));
        assertThat(jdbc.queryForObject("SELECT revoked_at FROM identity_session WHERE id = ?", LocalDateTime.class,
                accessClaims(absolute).getStringClaim("sid"))).isNull();
    }

    @Test
    void rejectsUntrustedOriginsAndLogsOutIdempotentlyWithoutAffectingAnotherDevice() throws Exception {
        var original = sessionLogin("192.0.2.108");
        var anotherDevice = sessionLogin("192.0.2.109");
        String cookie = refreshCookie(original);
        var untrustedRefresh = cookiePost("/refresh", cookie, "https://other.example");
        assertProblem(untrustedRefresh, 403, "FORBIDDEN");
        assertThat(untrustedRefresh.headers().firstValue("Set-Cookie")).isEmpty();
        var untrustedLogout = cookiePost("/logout", cookie, null);
        assertProblem(untrustedLogout, 403, "FORBIDDEN");
        assertThat(untrustedLogout.headers().firstValue("Set-Cookie")).isEmpty();
        assertProblem(cookiePost("/refresh", null, ORIGIN), 401, "UNAUTHENTICATED");

        var rotated = cookiePost("/refresh", cookie, ORIGIN);
        String successor = refreshCookie(rotated);
        assertLoggedOut(cookiePost("/logout", cookie, ORIGIN));
        assertLoggedOut(cookiePost("/logout", successor, ORIGIN));
        assertLoggedOut(cookiePost("/logout", null, ORIGIN));
        assertLoggedOut(cookiePost("/logout", "portal_refresh_token=unknown", ORIGIN));
        assertLoggedOut(cookiePost("/logout", "portal_refresh_token=" + "a".repeat(43), ORIGIN));

        assertProblem(cookiePost("/refresh", successor, ORIGIN), 401, "UNAUTHENTICATED");
        assertThat(get(AUTH + "/me", accessToken(original)).statusCode()).isEqualTo(200);
        assertThat(cookiePost("/refresh", refreshCookie(anotherDevice), ORIGIN).statusCode()).isEqualTo(200);
    }

    @Test
    void rollsBackRotationAndKeepsLogoutCookieWhenTheSessionWriteFails() throws Exception {
        var original = sessionLogin("192.0.2.110");
        String cookie = refreshCookie(original);
        String sessionId = accessClaims(original).getStringClaim("sid");
        jdbc.execute("""
                CREATE TRIGGER portal_test_reject_session_write BEFORE UPDATE ON identity_session
                FOR EACH ROW
                BEGIN
                    IF OLD.id = '%s' THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Portal test rejected session write';
                    END IF;
                END
                """.formatted(sessionId));
        try {
            var failed = cookiePost("/refresh", cookie, ORIGIN);
            assertProblem(failed, 500, "INTERNAL_ERROR");
            assertThat(failed.headers().firstValue("Set-Cookie")).isEmpty();
            assertThat(tokenCount(sessionId)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT consumed_at FROM identity_refresh_token WHERE token_hash = ?",
                    LocalDateTime.class, cookieHash(cookie))).isNull();
            var unavailable = cookiePost("/logout", cookie, ORIGIN);
            assertProblem(unavailable, 503, "AUTH_STATE_UNAVAILABLE");
            assertThat(unavailable.headers().firstValue("Set-Cookie")).isEmpty();
        } finally {
            jdbc.execute("DROP TRIGGER portal_test_reject_session_write");
        }
        assertThat(cookiePost("/refresh", cookie, ORIGIN).statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> sessionLogin(String ip) throws Exception {
        redis.delete(List.of(REDIS_PREFIX + "phone:" + PHONE, REDIS_PREFIX + "ip:" + ip));
        var response = login(PHONE, PASSWORD, ip);
        assertThat(response.statusCode()).isEqualTo(200);
        return response;
    }

    private int tokenCount(String sessionId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM identity_refresh_token WHERE session_id = ?", Integer.class, sessionId);
    }

    private String accessToken(HttpResponse<String> response) {
        return json.readTree(response.body()).path("data").path("accessToken").asString();
    }

    private JWTClaimsSet accessClaims(HttpResponse<String> response) throws Exception {
        return SignedJWT.parse(accessToken(response)).getJWTClaimsSet();
    }

    private static String refreshCookie(HttpResponse<String> response) {
        return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
    }

    private static byte[] cookieHash(String cookie) throws Exception {
        String token = cookie.substring("portal_refresh_token=".length());
        return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
    }

    private HttpResponse<String> cookiePost(String action, String cookie, String origin) throws Exception {
        return client.send(cookieRequest(action, cookie, origin), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest cookieRequest(String action, String cookie, String origin) {
        var request = HttpRequest.newBuilder(uri(AUTH + action)).POST(HttpRequest.BodyPublishers.noBody());
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        if (origin != null) {
            request.header("Origin", origin);
        }
        return request.build();
    }

    private void assertLoggedOut(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.readTree(response.body()).path("data").path("loggedOut").asBoolean()).isTrue();
        assertThat(response.headers().firstValue("Set-Cookie").orElseThrow())
                .startsWith("portal_refresh_token=;").contains("Max-Age=0", "HttpOnly", "SameSite=Lax",
                        "Path=/api/v1/auth/portal");
    }

    /** 签发用于响应分类测试的同密钥 JWT。 */
    private String fixtureToken(RSAKey key, String audience, Instant expiresAt) {
        var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build();
        var claims = JwtClaimsSet.builder().issuer(ISSUER).subject("1").audience(List.of(audience))
                .issuedAt(expiresAt.minusSeconds(600)).expiresAt(expiresAt)
                .claim("client_type", "PORTAL").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private HttpResponse<String> login(String phone, String password, String ip) throws Exception {
        return post(json.writeValueAsString(Map.of("phone", phone, "password", password)), ORIGIN, ip);
    }

    private HttpResponse<String> post(String body, String origin, String ip) throws Exception {
        return client.send(loginRequest(body, origin, ip), HttpResponse.BodyHandlers.ofString());
    }

    /** 构造直连 Identity 的测试请求，由本机受信对端传入来源地址。 */
    private HttpRequest loginRequest(String body, String origin, String ip) {
        return HttpRequest.newBuilder(uri(AUTH + "/login"))
                .header("Origin", origin).header("Content-Type", "application/json")
                .header("X-Identity-Client-IP", ip).POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }

    private HttpResponse<String> get(String path, String accessToken) throws Exception {
        var request = HttpRequest.newBuilder(uri(path));
        if (accessToken != null) {
            request.header("Authorization", "Bearer " + accessToken);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private JsonNode assertProblem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type")).contains("application/problem+json");
        var problem = json.readTree(response.body());
        assertThat(problem.path("code").asString()).isEqualTo(code);
        assertThat(problem.path("traceId").asString()).matches("[a-f0-9]{32}");
        assertThat(problem.has("instance")).isFalse();
        return problem;
    }
}
