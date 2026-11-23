package com.swordshield.next.gateway;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.gateway.server.webflux.httpclient.response-timeout=1s"
})
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class GatewayIdentityRoutingTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String HEALTH_PATH = "/api/internal/identity/health";
    private static final String UPSTREAM_PROBLEM = """
            {"type":"about:blank","title":"服务不可用","status":503,"detail":"上游当前不可用",+            "code":"SERVICE_UNAVAILABLE","traceId":"4bf92f3577b34da6a3ce929d0e0e4736",+            "timestamp":"2026-11-23T00:00:00Z"}
            """;
    private static final HttpServer UPSTREAM = startUpstream();

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper jsonMapper = new JsonMapper();

    /** 将真实 Gateway 路由指向测试用 HTTP 服务。 */
    @DynamicPropertySource
    static void upstreamAddress(DynamicPropertyRegistry registry) {
        registry.add("gateway.identity-uri", () -> "http://127.0.0.1:" + UPSTREAM.getAddress().getPort());
    }

    /** 验证上游追踪传播、响应透传、超时和连接失败。 */
    @Test
    void routesHealthAndReportsUpstreamFailures(CapturedOutput output) throws Exception {
        try {
            // 1. 请求实际路由，确认上游收到同一 trace 并创建了新的 span。
            var health = request("healthy");
            assertThat(health.statusCode()).isEqualTo(200);
            assertThat(health.body()).matches("00-" + TRACE_ID + "-[0-9a-f]{16}-01")
                    .isNotEqualTo("00-" + TRACE_ID + "-00f067aa0ba902b7-01");

            // 2. 让上游直接返回 Problem Details，确认 Gateway 原样透传。
            var upstreamProblem = request("problem");
            assertThat(upstreamProblem.statusCode()).isEqualTo(503);
            assertThat(upstreamProblem.headers().firstValue("Content-Type")).contains("application/problem+json");
            assertThat(upstreamProblem.body()).isEqualTo(UPSTREAM_PROBLEM);

            // 3. 触发网关路由、上游提前关连接和超时错误。
            assertProblem(request("/api/internal/identity/missing", "healthy"), 404, "RESOURCE_NOT_FOUND");
            assertProblem(request("closed"), 502, "BAD_GATEWAY");
            assertProblem(request("delayed"), 504, "GATEWAY_TIMEOUT");
        } finally {
            UPSTREAM.stop(0);
        }

        // 4. 停止上游后复测同一路由，对照响应和网关日志的追踪 ID。
        assertProblem(request("healthy"), 503, "SERVICE_UNAVAILABLE");
        assertThat(output.getOut()).contains("SERVICE_UNAVAILABLE").contains(TRACE_ID);
    }

    /** 在随机端口启动仅用于测试的上游 HTTP 服务。 */
    private static HttpServer startUpstream() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/actuator/health", GatewayIdentityRoutingTest::respond);
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException("无法启动测试用 Identity 上游", ex);
        }
    }

    /** 模拟健康响应、上游错误、中断和超时。 */
    private static void respond(HttpExchange exchange) throws IOException {
        try (exchange) {
            // 1. 按测试场景提前结束连接，或等待超过 Gateway 的期限。
            String scenario = exchange.getRequestHeaders().getFirst("X-Test-Upstream");
            if ("closed".equals(scenario)) {
                return;
            }
            if ("delayed".equals(scenario)) {
                try {
                    Thread.sleep(Duration.ofSeconds(3).toMillis());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                return;
            }

            // 2. 回传上游收到的 traceparent，或返回用于透传比对的响应。
            boolean problem = "problem".equals(scenario);
            String body = problem ? UPSTREAM_PROBLEM : exchange.getRequestHeaders().getFirst("traceparent");
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", problem ? "application/problem+json" : "text/plain");
            exchange.sendResponseHeaders(problem ? 503 : 200, payload.length);
            exchange.getResponseBody().write(payload);
        }
    }

    /** 请求开发和测试环境的 Identity 健康转发入口。 */
    private HttpResponse<String> request(String scenario) throws Exception {
        return request(HEALTH_PATH, scenario);
    }

    /** 请求指定的 Gateway 路径，并附带追踪与测试场景信息。 */
    private HttpResponse<String> request(String path, String scenario) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5))
                .header("traceparent", "00-" + TRACE_ID + "-00f067aa0ba902b7-01")
                .header("X-Test-Upstream", scenario)
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** 核对网关错误的 HTTP 状态、必要字段与真实请求追踪。 */
    private void assertProblem(HttpResponse<String> response, int status, String code) {
        // 1. 核对媒体类型、公开字段和能够解析的响应时间。
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type")).contains("application/problem+json");
        var problem = jsonMapper.readTree(response.body());
        assertThat(problem.path("type").asString()).isEqualTo("about:blank");
        assertThat(problem.path("status").asInt()).isEqualTo(status);
        assertThat(problem.path("code").asString()).isEqualTo(code);
        assertThat(problem.path("title").asString()).isNotBlank();
        assertThat(problem.path("detail").asString()).isNotBlank();
        assertThat(problem.path("traceId").asString()).isEqualTo(TRACE_ID);
        assertThat(Instant.parse(problem.path("timestamp").asString())).isNotNull();
        assertThat(problem.has("instance")).isFalse();
    }
}
