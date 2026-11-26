package com.swordshield.next.gateway;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

final class AdminForwardAuth implements GatewayFilter {

    private static final String ACCOUNT_ID = "X-Admin-Account-Id";
    private static final String ROLES = "X-Admin-Roles";
    private static final String CSRF = "X-CSRF-TOKEN";
    private static final String VALIDATE = "/internal/admin/session/validate";

    private final WebClient identity;
    private final JsonMapper json = new JsonMapper();

    AdminForwardAuth(ObservationRegistry observations, String identityUri) {
        identity = WebClient.builder().observationRegistry(observations).baseUrl(identityUri).build();
    }

    /** 同步等待 Identity 返回当前账号和角色，校验成功后才继续业务路由。 */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 传递原方法、必要会话凭证和 CSRF 证明。WebClient 延续当前追踪。
        var incoming = exchange.getRequest();
        var request = identity.method(incoming.getMethod()).uri(VALIDATE)
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON)
                .headers(headers -> {
                    headers.addAll(HttpHeaders.ORIGIN, incoming.getHeaders().getOrEmpty(HttpHeaders.ORIGIN));
                    headers.addAll(CSRF, incoming.getHeaders().getOrEmpty(CSRF));
                });
        var session = incoming.getCookies().getFirst(AuthCookies.ADMIN);
        if (session != null) {
            request.cookie(session.getName(), session.getValue());
        }

        // 校验超时只约束认证调用，业务路由继续使用网关已有的上游响应超时。
        return request.exchangeToMono(response -> response.toEntity(byte[].class))
                .timeout(Duration.ofSeconds(5))
                .onErrorMap(TimeoutException.class, error -> new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, null, error))
                .onErrorMap(WebClientRequestException.class,
                        error -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, null, error))
                .flatMap(result -> {
                    // 2. 拒绝时直接响应。成功时覆盖外部身份头并移除认证凭证。
                    if (result.getStatusCode() != HttpStatus.OK) {
                        return reject(exchange, result);
                    }
                    var verified = readIdentity(result.getBody());
                    var trusted = incoming.mutate().headers(headers -> {
                        removeIdentity(headers);
                        headers.set(ACCOUNT_ID, verified.accountId());
                        headers.set(ROLES, String.join(",", verified.roles()));
                        headers.remove(HttpHeaders.AUTHORIZATION);
                        headers.remove(CSRF);
                        AuthCookies.remove(headers, AuthCookies.ADMIN, AuthCookies.PORTAL);
                    }).build();
                    return chain.filter(exchange.mutate().request(trusted).build());
                });
    }

    /** 删除外部送来的所有同名身份头。 */
    static void removeIdentity(HttpHeaders headers) {
        headers.remove(ACCOUNT_ID);
        headers.remove(ROLES);
    }

    /** 透传 Identity 的已知拒绝响应，意外的上游协议响应交给网关错误处理器。 */
    private static Mono<Void> reject(ServerWebExchange exchange, ResponseEntity<byte[]> result) {
        MediaType contentType = result.getHeaders().getContentType();
        if (!result.getStatusCode().isError() || contentType == null
                || !MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(contentType) || result.getBody() == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY));
        }
        var response = exchange.getResponse();
        response.setStatusCode(result.getStatusCode());
        response.getHeaders().setContentType(contentType);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(result.getBody())));
    }

    /** 在网关边界验证身份响应完整性，避免构造空的可信身份。 */
    private ValidatedSession readIdentity(byte[] body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
        }
        try {
            var session = json.readValue(body, ValidatedSession.class);
            if (session == null || session.accountId() == null || session.accountId().isBlank()
                    || session.roles() == null || session.roles().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
            }
            return session;
        } catch (JacksonException error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, null, error);
        }
    }

    private record ValidatedSession(String accountId, List<String> roles) {}
}
