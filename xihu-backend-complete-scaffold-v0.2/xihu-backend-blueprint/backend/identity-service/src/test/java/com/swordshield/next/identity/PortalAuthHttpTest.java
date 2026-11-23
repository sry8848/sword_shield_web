package com.swordshield.next.identity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
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
        jdbc.update("INSERT INTO identity_account (phone, password_hash, status) "
                + "SELECT ?, password_hash, 'DISABLED' FROM identity_account WHERE phone = ?", disabled, PHONE);
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
