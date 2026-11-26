package com.swordshield.next.activity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.address=127.0.0.1")
@ActiveProfiles("test")
@Import(AdminGatewayFlowTest.Fixture.class)
@EnabledIfEnvironmentVariable(named = "ADMIN_GATEWAY_INTEGRATION", matches = "true")
class AdminGatewayFlowTest {

    private static final String AUTH = "/api/v1/auth/admin";
    private static final String ORIGIN = "http://localhost:5174";
    private static final String EVENTS = "/api/v1/admin/events";
    private static final String CHECK_INS = "/api/v1/admin/check-ins";
    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

    @LocalServerPort
    private int activityPort;

    @TempDir
    private Path temporary;

    private int gatewayPort;
    private final List<Process> processes = new ArrayList<>();
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = new JsonMapper();

    /** 使用真实 Gateway、Identity、Activity 及 Redis/MySQL 验证后台请求链路。 */
    @Test
    void enforcesCurrentAdminIdentityAndCsrfBeforeActivity() throws Exception {
        // 1. 启动实际可执行包。Activity 受保护端点仅由当前测试上下文提供。
        Path key = temporary.resolve("portal-signing.pem");
        var keys = KeyPairGenerator.getInstance("RSA");
        keys.initialize(2048);
        String privateKey = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(keys.generateKeyPair().getPrivate().getEncoded());
        Files.writeString(key, "-----BEGIN PRIVATE KEY-----\n" + privateKey + "\n-----END PRIVATE KEY-----\n");
        int identityPort = start("identity-service", "--identity.portal.private-key=" + key.toUri(),
                "--spring.data.redis.database=1");
        gatewayPort = start("gateway", "--gateway.identity-uri=http://127.0.0.1:" + identityPort,
                "--gateway.activity-uri=http://127.0.0.1:" + activityPort);

        // 2. 内部接口无公共路由，外部身份头和 Portal Bearer 没有 Admin 会话权限。
        expect(send("GET", "/internal/admin/session/validate", Map.of(), null), 404);
        Map<String, String> spoofed = Map.of("X-Admin-Account-Id", "external-account",
                "X-Admin-Roles", "EVENT_OPERATOR");
        assertProblem(send("GET", EVENTS, spoofed, null), 401, "UNAUTHENTICATED");
        var portal = expect(send("POST", "/api/v1/auth/portal/login",
                Map.of("Origin", "http://localhost:5173"),
                "{\"phone\":\"13800000001\",\"password\":\"LocalPortal-Only!2026\"}"), 200);
        String bearer = "Bearer " + json.readTree(portal.body()).at("/data/accessToken").asString();
        assertProblem(send("GET", EVENTS, Map.of("Authorization", bearer), null), 401, "UNAUTHENTICATED");

        // 3. 运营账号访问活动时收到可信身份。写入时原始请求体由 Activity 接收。
        var operator = login("13800000004", "LocalOperator-Only!2026");
        var read = expect(send("GET", EVENTS, Map.of("Cookie", operator.cookie() + "; extra=kept",
                "Authorization", bearer, "X-Admin-Account-Id", "external-account",
                "X-Admin-Roles", "SUPER_ADMIN"), null), 200);
        var forwarded = json.readTree(read.body());
        assertThat(forwarded.path("accountId").asString()).isEqualTo(operator.id());
        assertThat(forwarded.path("roles").toString()).isEqualTo("[\"ROLE_EVENT_OPERATOR\"]");
        assertThat(forwarded.path("cookie").asString()).isEqualTo("extra=kept");
        assertThat(forwarded.path("authorization").asString()).isEmpty();
        assertThat(forwarded.path("traceId").asString()).isEqualTo(TRACE_ID);
        assertProblem(send("POST", EVENTS, Map.of("Cookie", operator.cookie(), "Origin", ORIGIN),
                "{\"title\":\"Gateway integration\"}"), 403, "FORBIDDEN");
        assertProblem(send("POST", EVENTS, Map.of("Cookie", operator.cookie(),
                "Origin", "http://localhost:9999", "X-CSRF-TOKEN", operator.csrf()),
                "{\"title\":\"Gateway integration\"}"), 403, "FORBIDDEN");
        var written = expect(send("POST", EVENTS, Map.of("Cookie", operator.cookie(),
                "Origin", ORIGIN, "X-CSRF-TOKEN", operator.csrf()),
                "{\"title\":\"Gateway integration\"}"), 200);
        assertThat(json.readTree(written.body()).path("title").asString()).isEqualTo("Gateway integration");

        // 4. 角色在每次鉴权时实时读取。兼任获两种权限，超级管理员仍按自身角色范围授权。
        expect(send("GET", CHECK_INS, Map.of("Cookie", operator.cookie()), null), 403);
        try (var connection = DriverManager.getConnection(System.getenv("IDENTITY_TEST_DB_URL"),
                System.getenv("IDENTITY_TEST_DB_USERNAME"), System.getenv("IDENTITY_TEST_DB_PASSWORD"));
                var grant = connection.prepareStatement("""
                        INSERT INTO identity_account_role (account_id, role_id)
                        SELECT ?, id FROM identity_role WHERE code = 'CHECKIN_STAFF'
                        """);
                var revoke = connection.prepareStatement("""
                        DELETE FROM identity_account_role WHERE account_id = ?
                        AND role_id = (SELECT id FROM identity_role WHERE code = 'CHECKIN_STAFF')
                        """)) {
            grant.setString(1, operator.id());
            revoke.setString(1, operator.id());
            grant.executeUpdate();
            try {
                expect(send("GET", CHECK_INS, Map.of("Cookie", operator.cookie()), null), 200);
                expect(send("GET", EVENTS, Map.of("Cookie", operator.cookie()), null), 200);
            } finally {
                revoke.executeUpdate();
            }
        }
        var admin = login("13800000002", "LocalAdmin-Only!2026");
        expect(send("GET", EVENTS, Map.of("Cookie", admin.cookie()), null), 403);
        expect(send("GET", CHECK_INS, Map.of("Cookie", admin.cookie()), null), 403);
        expect(send("POST", AUTH + "/logout",
                Map.of("Origin", ORIGIN, "Cookie", operator.cookie(), "X-CSRF-TOKEN", operator.csrf()), null), 200);
        assertProblem(send("GET", EVENTS, Map.of("Cookie", operator.cookie()), null), 401, "UNAUTHENTICATED");
    }

    @AfterEach
    void stopServices() {
        for (Process process : processes) {
            process.destroyForcibly();
        }
    }

    private AdminSession login(String phone, String password) throws Exception {
        var anonymous = expect(send("GET", AUTH + "/csrf", Map.of(), null), 200);
        var authenticated = expect(send("POST", AUTH + "/login", Map.of("Origin", ORIGIN,
                "Cookie", cookie(anonymous), "X-CSRF-TOKEN", json.readTree(anonymous.body()).at("/data/token").asString()),
                json.writeValueAsString(Map.of("phone", phone, "password", password))), 200);
        String cookie = cookie(authenticated);
        var token = expect(send("GET", AUTH + "/csrf", Map.of("Cookie", cookie), null), 200);
        return new AdminSession(cookie, json.readTree(token.body()).at("/data/token").asString(),
                json.readTree(authenticated.body()).at("/data/user/id").asString());
    }

    private int start(String module, String... configuration) throws Exception {
        Path jar = Path.of("..", module, "target", module + "-0.1.0-SNAPSHOT.jar").toAbsolutePath().normalize();
        assertThat(jar).as("先构建 Gateway 和 Identity 的可执行包，再运行跨服务验收").exists();
        int port;
        try (var socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            port = socket.getLocalPort();
        }
        var arguments = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-jar", jar.toString(), "--spring.profiles.active=test", "--server.address=127.0.0.1",
                "--server.port=" + port));
        arguments.addAll(List.of(configuration));
        Path log = temporary.resolve(module + ".log");
        var process = new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        processes.add(process);
        var health = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
                .timeout(Duration.ofSeconds(2)).build();
        long deadline = System.nanoTime() + Duration.ofSeconds(50).toNanos();
        while (process.isAlive() && System.nanoTime() < deadline) {
            try {
                if (client.send(health, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    return port;
                }
            } catch (IOException starting) {
                // 端口绑定前继续等待已启动的子进程。
            }
            Thread.sleep(300);
        }
        throw new IllegalStateException(module + " 未就绪\n" + Files.readString(log));
    }

    private HttpResponse<String> send(String method, String path, Map<String, String> headers, String payload)
            throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + gatewayPort + path))
                .timeout(Duration.ofSeconds(10)).header("traceparent", "00-" + TRACE_ID + "-00f067aa0ba902b7-01");
        headers.forEach(request::header);
        if (payload != null) {
            request.header("Content-Type", "application/json");
        }
        request.method(method, payload == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(payload));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertProblem(HttpResponse<String> response, int status, String code) {
        expect(response, status);
        var problem = json.readTree(response.body());
        assertThat(problem.path("code").asString()).isEqualTo(code);
        assertThat(problem.path("traceId").asString()).isEqualTo(TRACE_ID);
    }

    private static HttpResponse<String> expect(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).as("%s", response.uri().getPath()).isEqualTo(status);
        return response;
    }

    private static String cookie(HttpResponse<String> response) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(value -> value.startsWith("admin_session="))
                .findFirst().orElseThrow().split(";", 2)[0];
    }

    private record AdminSession(String cookie, String csrf, String id) {}

    @TestConfiguration(proxyBeanMethods = false)
    @Import(ActivityEndpoints.class)
    static class Fixture {

        /** 验收期间仅通过本机回环连接接收网关身份，测试端点随测试源码编译。 */
        @Bean
        SecurityFilterChain activitySecurity(HttpSecurity http) throws Exception {
            return http.securityMatcher("/api/v1/admin/**")
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(rules -> rules
                            .requestMatchers(HttpMethod.GET, CHECK_INS).hasRole("CHECKIN_STAFF")
                            .requestMatchers(EVENTS).hasRole("EVENT_OPERATOR")
                            .anyRequest().denyAll())
                    .addFilterBefore(new OncePerRequestFilter() {
                        @Override
                        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                FilterChain chain) throws IOException, ServletException {
                            String account = request.getHeader("X-Admin-Account-Id");
                            String roles = request.getHeader("X-Admin-Roles");
                            if ("127.0.0.1".equals(request.getRemoteAddr()) && account != null && roles != null) {
                                var authorities = java.util.Arrays.stream(roles.split(","))
                                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
                                SecurityContextHolder.getContext().setAuthentication(
                                        UsernamePasswordAuthenticationToken.authenticated(account, null, authorities));
                            }
                            chain.doFilter(request, response);
                        }
                    }, AuthorizationFilter.class).build();
        }
    }

    @RestController
    static class ActivityEndpoints {
        private final Tracer tracer;

        ActivityEndpoints(Tracer tracer) {
            this.tracer = tracer;
        }

        @GetMapping({EVENTS, CHECK_INS})
        Map<String, Object> read(Authentication auth, HttpServletRequest request) {
            return Map.of("accountId", auth.getName(),
                    "roles", auth.getAuthorities().stream().map(Object::toString).toList(),
                    "cookie", request.getHeader("Cookie") == null ? "" : request.getHeader("Cookie"),
                    "authorization", request.getHeader("Authorization") == null ? "" : request.getHeader("Authorization"),
                    "traceId", tracer.currentSpan().context().traceId());
        }

        @PostMapping(EVENTS)
        Map<String, Object> write(@RequestBody Map<String, Object> payload) {
            return payload;
        }
    }
}
