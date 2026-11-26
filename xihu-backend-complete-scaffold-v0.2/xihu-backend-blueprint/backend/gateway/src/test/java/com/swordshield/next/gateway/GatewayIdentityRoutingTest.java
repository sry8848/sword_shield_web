package com.swordshield.next.gateway;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.gateway.server.webflux.httpclient.response-timeout=1s"
})
@ActiveProfiles("test")
@Import(GatewayIdentityRoutingTest.UnavailableRoutes.class)
@ExtendWith(OutputCaptureExtension.class)
class GatewayIdentityRoutingTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String HEALTH_PATH = "/api/internal/identity/health";
    private static final String UPSTREAM_PROBLEM = """
            {
              "type": "about:blank",
              "title": "服务不可用",
              "status": 503,
              "detail": "上游当前不可用",
              "code": "SERVICE_UNAVAILABLE",
              "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
              "timestamp": "2026-11-23T00:00:00Z"
            }
            """;

    private static HttpServer upstream;
    private static ExecutorService upstreamRequests;
    private static SocketChannel closedUpstream;

    @LocalServerPort
    private int port;

    @Autowired
    private GatewayProblemHandler problemHandler;

    @Autowired
    private Tracer tracer;

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper jsonMapper = new JsonMapper();

    /** 让一个真实 HTTP 服务覆盖全类测试，并保留一个绑定但不监听的端口。 */
    @BeforeAll
    static void startUpstreams() throws IOException {
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        upstream.createContext("/actuator/health", GatewayIdentityRoutingTest::respond);
        upstream.createContext("/api/v1/auth/portal/login", GatewayIdentityRoutingTest::respond);
        upstream.createContext("/api/v1/auth/admin/csrf", GatewayIdentityRoutingTest::respond);
        // 超时用例会延迟响应，其他用例仍可使用同一个测试服务。
        upstreamRequests = Executors.newCachedThreadPool();
        upstream.setExecutor(upstreamRequests);
        upstream.start();
        closedUpstream = SocketChannel.open().bind(new InetSocketAddress("127.0.0.1", 0));
    }

    /** 释放测试服务及在测试期间保留的端口。 */
    @AfterAll
    static void stopUpstreams() throws IOException {
        if (upstream != null) {
            upstream.stop(0);
        }
        if (upstreamRequests != null) {
            upstreamRequests.shutdownNow();
        }
        if (closedUpstream != null) {
            closedUpstream.close();
        }
    }

    /** 将真实 Gateway 路由指向测试用 HTTP 服务。 */
    @DynamicPropertySource
    static void upstreamAddress(DynamicPropertyRegistry registry) {
        registry.add("gateway.identity-uri", () -> "http://127.0.0.1:" + upstream.getAddress().getPort());
    }

    /** 验证 Gateway 对上游的追踪传播、来源边界和合法错误响应透传。 */
    @Test
    void routesIdentityAndPreservesUpstreamProblem() throws Exception {
        // 1. 确认上游收到同一 trace，且登录路由重建其受信来源地址。
        var health = request("healthy");
        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(health.body()).matches("00-" + TRACE_ID + "-[0-9a-f]{16}-01")
                .isNotEqualTo("00-" + TRACE_ID + "-00f067aa0ba902b7-01");
        var login = request("/api/v1/auth/portal/login", "source-address");
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).isEqualTo("127.0.0.1");

        // 2. 先验证实际返回的是合法 Problem Details，再比对原始响应体。
        var upstreamProblem = request("problem");
        assertProblem(upstreamProblem, 503, "SERVICE_UNAVAILABLE");
        assertThat(upstreamProblem.body()).isEqualTo(UPSTREAM_PROBLEM);
    }

    /** 同一主机上的两种认证 Cookie 只进对应应用，其他 Cookie 原样保留。 */
    @Test
    void isolatesApplicationCookies() throws Exception {
        for (String[] route : new String[][]{
                {"/api/v1/auth/portal/login", "portal_refresh_token=portal; extra=keep"},
                {"/api/v1/auth/admin/csrf", "admin_session=admin; extra=keep"}}) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + route[0]))
                    .header("X-Test-Upstream", "cookies")
                    .header("Cookie", "admin_session=admin; portal_refresh_token=portal; extra=keep")
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo(route[1]);
        }
    }

    /** 验证当前 Gateway 版本的连接失败、提前关闭和超时行为。 */
    @Test
    void classifiesUpstreamFailuresAndRecordsSafeDiagnostics(CapturedOutput output) throws Exception {
        assertProblem(request("/api/internal/identity/missing", "healthy"), 404, "RESOURCE_NOT_FOUND");
        assertProblem(request("/test/upstream/refused", "healthy"), 503, "SERVICE_UNAVAILABLE");
        assertProblem(request("closed"), 502, "BAD_GATEWAY");
        assertProblem(request("delayed"), 504, "GATEWAY_TIMEOUT");
        assertThat(output.getOut().lines().filter(line -> line.contains("identity-refused-test")).toList())
                .singleElement().satisfies(line -> {
                    assertThat(line).doesNotContain("127.0.0.1");
                    var access = jsonMapper.readTree(line);
                    assertThat(access.path("trace.id").asString()).isEqualTo(TRACE_ID);
                    assertThat(access.path("gateway").path("target").path("service").asString()).isEqualTo("identity-service");
                    assertThat(access.path("http").path("response").path("status_code").asInt()).isEqualTo(503);
                    assertThat(access.path("event").path("duration").asLong()).isPositive();
                    assertThat(access.path("error").path("type").asString()).contains("ConnectException");
                });
    }

    /** 验证 HTTP 入口为缺失或格式无效的 traceparent 建立真实请求追踪。 */
    @Test
    void startsHttpTracesWithoutValidIncomingContext() throws Exception {
        for (String traceparent : new String[]{null, "invalid"}) {
            var response = request("/api/internal/identity/missing", "healthy", traceparent);
            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(jsonMapper.readTree(response.body()).path("traceId").asString()).matches("[a-f0-9]{32}");
        }
    }

    /** 验证错误处理器可在当前线程没有 span 时恢复 HTTP Observation 上下文。 */
    @Test
    void recoversRequestSpanFromObservation() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(HEALTH_PATH));
        var span = tracer.nextSpan().name("test-request").start();
        try {
            var observation = new ServerRequestObservationContext(
                    exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
            var tracing = new TracingContext();
            tracing.setSpan(span);
            observation.put(TracingContext.class, tracing);
            exchange.getAttributes().put(ServerRequestObservationContext.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, observation);
            assertThat(tracer.currentSpan()).isNull();
            problemHandler.handle(exchange, new ConnectException()).block();
            assertThat(jsonMapper.readTree(exchange.getResponse().getBodyAsString().block())
                    .path("traceId").asString()).isEqualTo(span.context().traceId());
        } finally {
            span.end();
        }
    }

    /** 验证连 HTTP Observation 都缺失时仍返回原状态，并为错误处理生成真实追踪。 */
    @Test
    void handlesWrappedNetworkFailureWithoutRequestObservation(CapturedOutput output) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(HEALTH_PATH));
        assertThat(tracer.currentSpan()).isNull();
        problemHandler.handle(exchange, new IllegalStateException("下游包装异常", new UnknownHostException())).block();
        var body = jsonMapper.readTree(exchange.getResponse().getBodyAsString().block());
        String traceId = body.path("traceId").asString();
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(503);
        assertThat(body.path("code").asString()).isEqualTo("SERVICE_UNAVAILABLE");
        assertThat(traceId).matches("[a-f0-9]{32}");
        assertThat(output.getOut()).contains(traceId);
        assertThat(tracer.currentSpan()).isNull();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class UnavailableRoutes {
        /** 用未监听的本地端口复现连接拒绝。 */
        @Bean
        RouteLocator unavailableUpstreams(RouteLocatorBuilder routes) throws IOException {
            int unavailablePort = ((InetSocketAddress) closedUpstream.getLocalAddress()).getPort();
            return routes.routes()
                    .route("identity-refused-test", route -> route.path("/test/upstream/refused")
                            .metadata(GatewayAccessLog.TARGET_SERVICE, "identity-service")
                            .uri("http://127.0.0.1:" + unavailablePort))
                    .build();
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
            String body = switch (scenario) {
                case "problem" -> UPSTREAM_PROBLEM;
                case "cookies" -> exchange.getRequestHeaders().getFirst("Cookie");
                case "source-address" -> exchange.getRequestHeaders().getFirst("X-Identity-Client-IP");
                default -> exchange.getRequestHeaders().getFirst("traceparent");
            };
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
        return request(path, scenario, "00-" + TRACE_ID + "-00f067aa0ba902b7-01");
    }

    /** 支持缺失或非法追踪头，以验证 HTTP 入口创建新的追踪。 */
    private HttpResponse<String> request(String path, String scenario, String traceparent) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5))
                .header("X-Test-Upstream", scenario)
                .header("X-Identity-Client-IP", "192.0.2.42")
                .header("X-Forwarded-For", "192.0.2.43");
        if (traceparent != null) {
            request.header("traceparent", traceparent);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
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
