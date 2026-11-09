package com.swordshield.next.identity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

import com.swordshield.next.common.ApiEnvelope;
import io.micrometer.tracing.Tracer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.mvc.problemdetails.enabled=true",
        "APP_VERSION=identity-test"
})
@ActiveProfiles("test")
@Import(IdentityMvcErrorsTest.TestController.class)
@ExtendWith(OutputCaptureExtension.class)
class IdentityMvcErrorsTest {

    private static final String PATH = "/test/validation";
    private static final String SECRET = "sensitive-token-that-must-not-be-logged";

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext context;

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper mapper = new JsonMapper();

    /** 验证请求成功并带有本次请求的追踪标识。 */
    @Test
    void successfulRequestsHaveDistinctTraceIds() throws Exception {
        // 1. 发送两个独立的真实 HTTP 请求。
        var first = request("POST", PATH, "application/json", "{\"displayName\":\"有效\"}");
        var second = request("POST", PATH, "application/json", "{\"displayName\":\"有效\"}");

        // 2. 检查成功结构和每次请求的 Trace ID。
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(second.statusCode()).isEqualTo(200);
        JsonNode body = mapper.readTree(first.body());
        assertThat(body.path("code").asString()).isEqualTo("SUCCESS");
        assertThat(body.path("message").asString()).isEqualTo("操作成功");
        assertThat(body.path("data").asString()).isEqualTo("有效");
        assertThat(traceId(body)).isNotEqualTo(traceId(mapper.readTree(second.body())));
        assertThat(Instant.parse(body.path("timestamp").asString())).isBeforeOrEqualTo(Instant.now());
    }

    /** 验证在受控测试环境中接续有效的 W3C 上下文。 */
    @Test
    void continuesIncomingW3cTrace() throws Exception {
        // 1. 发送一个带有效 W3C 追踪头的直接请求。
        String suppliedTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + PATH + "?page=1"))
                .header("traceparent", "00-" + suppliedTraceId + "-00f067aa0ba902b7-01")
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 2. 验证当前 HTTP span 沿用入站 Trace ID。
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(traceId(mapper.readTree(response.body()))).isEqualTo(suppliedTraceId);
    }

    /** 验证调用方未提供有效 Trace ID 时，成功响应构造失败。 */
    @Test
    void successEnvelopeRequiresTraceId() {
        assertThatIllegalArgumentException().isThrownBy(() -> ApiEnvelope.ok("value", null))
                .withMessage("traceId must not be blank");
        assertThatIllegalArgumentException().isThrownBy(() -> ApiEnvelope.ok("value", " "))
                .withMessage("traceId must not be blank");
    }

    /** 验证 JSON 字段校验失败时只公开固定的字段说明。 */
    @Test
    void invalidBodyHasSafeFieldErrors(CapturedOutput output) throws Exception {
        // 1. 提交缺少必填字段的 JSON。
        var response = request("POST", PATH, "application/json", "{}");
        JsonNode body = problem(response, 400, "INVALID_ARGUMENT", "请求错误");

        // 2. 检查 errors 结构，并确认该请求没有完整异常日志。
        assertThat(body.path("errors").size()).isEqualTo(1);
        JsonNode field = body.path("errors").get(0);
        assertThat(field.size()).isEqualTo(3);
        assertThat(field.path("field").asString()).isEqualTo("displayName");
        assertThat(field.path("code").asString()).isEqualTo("REQUIRED");
        assertThat(field.path("detail").asString()).isEqualTo("缺少必填值");
        assertThat(output.getOut().lines().filter(line -> line.contains(traceId(body))))
                .noneMatch(line -> line.contains("\"stack_trace\""));
    }

    /** 验证 MVC 直接校验查询参数时返回对外参数名。 */
    @Test
    void invalidQueryParameterUsesTheRequestParameterName() throws Exception {
        var response = request("GET", PATH + "?page=0", null, "");
        JsonNode body = problem(response, 400, "INVALID_ARGUMENT", "请求错误");
        assertThat(body.path("errors").get(0).path("field").asString()).isEqualTo("page");
        assertThat(body.path("errors").get(0).path("code").asString()).isEqualTo("INVALID_VALUE");
    }

    /** 验证非法 JSON 不暴露输入或解析器细节。 */
    @Test
    void malformedJsonReturnsSafeProblem(CapturedOutput output) throws Exception {
        var response = request("POST", PATH, "application/json", "{\"displayName\": " + SECRET);
        JsonNode body = problem(response, 400, "INVALID_ARGUMENT", "请求错误");
        assertThat(body.path("detail").asString()).isEqualTo("请求体格式不正确");
        assertThat(body.has("errors")).isFalse();
        assertThat(response.body()).doesNotContain(SECRET, "Jackson");
        assertThat(output.getAll()).doesNotContain(SECRET);
    }

    /** 验证不支持的 HTTP 方法仍返回框架计算的 Allow 头。 */
    @Test
    void unsupportedMethodPreservesAllow() throws Exception {
        var response = request("PUT", PATH, "application/json", "{}");
        problem(response, 405, "METHOD_NOT_ALLOWED", "不支持的请求方法");
        assertThat(response.headers().firstValue("Allow")).hasValueSatisfying(
                allow -> assertThat(allow).contains("POST"));
    }

    /** 验证不支持的请求媒体类型返回 415。 */
    @Test
    void unsupportedMediaTypeReturnsProblem() throws Exception {
        problem(request("POST", PATH, "text/plain", "text"), 415,
                "UNSUPPORTED_MEDIA_TYPE", "不支持的媒体类型");
    }

    /** 验证 500 响应只返回通用说明，且可关联唯一一条保留原始异常的日志。 */
    @Test
    void unexpectedExceptionIsLoggedOnceWithTheResponseTraceId(CapturedOutput output) throws Exception {
        // 1. 触发带诊断消息和原因的异常，检查通用 500 响应。
        var response = request("GET", "/test/failure", null, "");
        JsonNode body = problem(response, 500, "INTERNAL_ERROR", "服务器内部错误");
        assertThat(body.path("detail").asString()).isEqualTo("服务器内部错误");
        assertThat(response.body()).doesNotContain("模拟业务执行失败", "模拟参数异常",
                "IllegalStateException", "IllegalArgumentException", "stack_trace", "TestController.failure");

        // 2. 取出该请求唯一的堆栈日志，核对 MDC 自动输出的追踪字段。
        var logs = output.getOut().lines()
                .filter(line -> line.contains(traceId(body))
                        && (line.contains("\"stack_trace\"") || line.contains("Identity MVC 未预期异常")))
                .map(mapper::readTree)
                .toList();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).path("log").path("level").asString()).isEqualTo("ERROR");
        assertThat(logs.get(0).path("service").path("name").asString()).isEqualTo("identity-service");
        assertThat(logs.get(0).path("service").path("version").asString()).isEqualTo("identity-test");
        assertThat(logs.get(0).path("trace.id").isString()).isTrue();
        assertThat(logs.get(0).path("span.id").isString()).isTrue();
        assertThat(logs.get(0).path("trace.id").asString()).isEqualTo(traceId(body));
        assertThat(logs.get(0).path("span.id").asString()).matches("[0-9a-f]{16}");
        assertThat(logs.get(0).has("traceId") || logs.get(0).has("spanId")).isFalse();
        JsonNode error = logs.get(0).path("error");
        assertThat(error.path("type").asString()).isEqualTo("java.lang.IllegalStateException");
        assertThat(error.path("message").asString()).isEqualTo("模拟业务执行失败");
        assertThat(error.path("stack_trace").asString())
                .contains("java.lang.IllegalStateException: 模拟业务执行失败", "TestController.failure",
                        "Caused by: java.lang.IllegalArgumentException: 模拟参数异常");
    }

    /** 验证未映射的路径使用统一的 404 错误响应。 */
    @Test
    void unmappedPathReturnsProblem() throws Exception {
        problem(request("GET", "/test/unmapped", null, ""), 404, "RESOURCE_NOT_FOUND", "未找到");
    }

    /** 即使启用 Boot 的 Problem Details 开关，也只有一个异常处理器。 */
    @Test
    void bootDefaultAdviceBacksOff() {
        assertThat(context.getBeansOfType(ResponseEntityExceptionHandler.class)).hasSize(1);
    }

    /**
     * 发送测试请求并返回 HTTP 响应，保留原始头和 JSON 字符串用于断言。
     */
    private HttpResponse<String> request(String method, String path, String contentType, String body) throws Exception {
        // 1. 构造与实际客户端一致的请求。
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body.isEmpty() ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * 核对通用错误契约，返回 JSON 供各场景检查特有字段。
     */
    private JsonNode problem(HttpResponse<String> response, int status, String code, String title) {
        // 1. 核对 HTTP 状态、媒体类型和响应字段。
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type")).contains("application/problem+json");
        JsonNode body = mapper.readTree(response.body());
        assertThat(body.path("status").asInt()).isEqualTo(status);
        assertThat(body.path("code").asString()).isEqualTo(code);
        assertThat(body.path("type").asString()).isEqualTo("about:blank");
        assertThat(body.path("title").asString()).isEqualTo(title);
        assertThat(body.path("detail").asString()).isNotBlank();
        assertThat(body.has("instance") || body.has("details")).isFalse();
        traceId(body);
        assertThat(Instant.parse(body.path("timestamp").asString())).isBeforeOrEqualTo(Instant.now());
        return body;
    }

    /** 从响应提取并校验 W3C 格式的 Trace ID。 */
    private static String traceId(JsonNode body) {
        String traceId = body.path("traceId").asString();
        assertThat(traceId).matches("[0-9a-f]{32}").isNotEqualTo("0".repeat(32));
        return traceId;
    }

    /** 为 HTTP 测试提供最小的输入校验与失败触发点。 */
    @RestController
    static class TestController {

        private final Tracer tracer;

        /** 使用真实 HTTP 请求的追踪上下文构造测试响应。 */
        TestController(Tracer tracer) {
            this.tracer = tracer;
        }

        /** 校验 JSON 后返回包含真实 Trace ID 的成功响应。 */
        @PostMapping(path = PATH, consumes = "application/json")
        ApiEnvelope<String> validateBody(@Valid @RequestBody ValidationInput input) {
            return ApiEnvelope.ok(input.displayName(), tracer.currentSpan().context().traceId());
        }

        /** 校验查询参数的最小值。 */
        @GetMapping(PATH)
        ApiEnvelope<Integer> validateQuery(@RequestParam("page") @Positive int pageNumber) {
            return ApiEnvelope.ok(pageNumber, tracer.currentSpan().context().traceId());
        }

        /** 触发带诊断消息和原因的未知异常。 */
        @GetMapping("/test/failure")
        void failure() {
            throw new IllegalStateException("模拟业务执行失败", new IllegalArgumentException("模拟参数异常"));
        }
    }

    /** 为请求体校验提供一个必填字段。 */
    record ValidationInput(@NotBlank String displayName) {}
}
