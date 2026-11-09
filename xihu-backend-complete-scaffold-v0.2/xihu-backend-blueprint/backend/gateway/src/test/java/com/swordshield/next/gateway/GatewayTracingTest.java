package com.swordshield.next.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "APP_VERSION=gateway-test")
@ActiveProfiles("test")
@Import(GatewayTracingTest.TraceEndpoint.class)
@ExtendWith(OutputCaptureExtension.class)
class GatewayTracingTest {

    private static final String TRACE_MESSAGE = "Gateway 测试请求";

    @LocalServerPort
    private int port;

    /** 验证 WebFlux 在异步线程内接续 W3C 上下文并输出关联的 ECS 日志。 */
    @Test
    void continuesIncomingW3cTraceAndLogsIt(CapturedOutput output) throws Exception {
        // 1. 通过 HTTP 向 Gateway 发送带有效追踪头的请求。
        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/test/trace"))
                .header("traceparent", "00-" + traceId + "-00f067aa0ba902b7-01")
                .build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        // 2. 对照响应和控制台事件，核对追踪字段及服务标识。
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(traceId);
        var mapper = new JsonMapper();
        var logs = output.getOut().lines()
                .filter(line -> line.contains(TRACE_MESSAGE))
                .map(mapper::readTree)
                .toList();
        assertThat(logs).singleElement().satisfies(log -> {
            assertThat(log.path("service").path("name").asString()).isEqualTo("gateway");
            assertThat(log.path("service").path("version").asString()).isEqualTo("gateway-test");
            assertThat(log.path("trace.id").isString()).isTrue();
            assertThat(log.path("trace.id").asString()).isEqualTo(response.body());
            assertThat(log.path("span.id").isString()).isTrue();
            assertThat(log.path("span.id").asString()).matches("[0-9a-f]{16}").isNotEqualTo("00f067aa0ba902b7");
            assertThat(log.has("traceId") || log.has("spanId")).isFalse();
        });
    }

    /** 验证 Gateway 当前 span 的测试端点，仅在测试上下文注册。 */
    @RestController
    record TraceEndpoint(Tracer tracer) {

        /** 切换执行线程后记录日志并返回当前 Trace ID。 */
        @GetMapping("/test/trace")
        Mono<String> trace() {
            // 1. 在异步线程验证 WebFlux 恢复了请求追踪上下文。
            return Mono.fromSupplier(() -> {
                LoggerFactory.getLogger(TraceEndpoint.class).info(TRACE_MESSAGE);
                return tracer.currentSpan().context().traceId();
            }).subscribeOn(Schedulers.boundedElastic());
        }
    }
}
