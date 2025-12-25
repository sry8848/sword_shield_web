package com.swordshield.admin.service;

import com.swordshield.admin.dto.ActivityDto;
import com.swordshield.common.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class ActivityAdminClient {
    private static final Logger log = LoggerFactory.getLogger(ActivityAdminClient.class);

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final String baseUrl;
    private final String serviceName;

    public ActivityAdminClient(RestTemplateBuilder builder,
                               DiscoveryClient discoveryClient,
                               @Value("${activity.service.base-url:}") String baseUrl,
                               @Value("${activity.service.name:sword-shield-activity}") String serviceName) {
        this.restTemplate = builder.build();
        this.discoveryClient = discoveryClient;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.serviceName = serviceName == null ? "" : serviceName.trim();
    }

    public Response<List<ActivityDto>> getAll(String authorization) {
        return exchangeWithFallback(
                base -> joinBaseUrl(base, "/api/activity/admin/activities"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(authorization)),
                new ParameterizedTypeReference<Response<List<ActivityDto>>>() {}
        );
    }

    public Response<Map<String, Object>> list(String authorization, int page, int size) {
        return exchangeWithFallback(
                base -> UriComponentsBuilder.fromHttpUrl(joinBaseUrl(base, "/api/activity/admin/activities/list"))
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(authorization)),
                new ParameterizedTypeReference<Response<Map<String, Object>>>() {}
        );
    }

    public Response<ActivityDto> getById(String authorization, Long id) {
        return exchangeWithFallback(
                base -> joinBaseUrl(base, "/api/activity/admin/activities/" + id),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(authorization)),
                new ParameterizedTypeReference<Response<ActivityDto>>() {}
        );
    }

    public Response<ActivityDto> create(String authorization, ActivityDto body) {
        return exchangeWithFallback(
                base -> joinBaseUrl(base, "/api/activity/admin/activities"),
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(authorization)),
                new ParameterizedTypeReference<Response<ActivityDto>>() {}
        );
    }

    public Response<ActivityDto> update(String authorization, Long id, ActivityDto body) {
        return exchangeWithFallback(
                base -> joinBaseUrl(base, "/api/activity/admin/activities/" + id),
                HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders(authorization)),
                new ParameterizedTypeReference<Response<ActivityDto>>() {}
        );
    }

    public Response<Object> delete(String authorization, Long id) {
        return exchangeWithFallback(
                base -> joinBaseUrl(base, "/api/activity/admin/activities/" + id),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(authorization)),
                new ParameterizedTypeReference<Response<Object>>() {}
        );
    }

    private HttpHeaders authHeaders(String authorization) {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.hasText(authorization)) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
        return headers;
    }

    private <T> Response<T> exchangeWithFallback(Function<String, String> urlBuilder,
                                                 HttpMethod method,
                                                 HttpEntity<?> request,
                                                 ParameterizedTypeReference<Response<T>> typeRef) {
        List<String> baseUrls = resolveCandidateBaseUrls();
        if (baseUrls.isEmpty()) {
            return Response.error(502, "Activity service unavailable");
        }

        Response<T> last = null;
        for (String baseUrlCandidate : baseUrls) {
            String url = urlBuilder.apply(baseUrlCandidate);
            try {
                ResponseEntity<Response<T>> response = restTemplate.exchange(url, method, request, typeRef);
                Response<T> body = response.getBody();
                if (body != null) {
                    return body;
                }
                last = Response.error(502, "Activity service empty response");
            } catch (RestClientResponseException e) {
                int status = e.getRawStatusCode();
                String bodySnippet = safeSnippet(e.getResponseBodyAsString(), 500);
                log.warn("Activity admin call failed: baseUrl={} url={} status={} body={}", baseUrlCandidate, url, status, bodySnippet);
                last = Response.error(status, "Activity service error");
                if (status == 401 || status == 403) {
                    return last;
                }
            } catch (Exception e) {
                log.warn("Activity admin call failed: baseUrl={} url={} error={}", baseUrlCandidate, url, e.getMessage());
                last = Response.error(502, "Activity service unavailable");
            }
        }
        return last == null ? Response.error(502, "Activity service unavailable") : last;
    }

    private String resolveBaseUrl() {
        if (discoveryClient == null || !StringUtils.hasText(serviceName)) {
            return StringUtils.hasText(baseUrl) ? baseUrl : null;
        }
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances == null || instances.isEmpty()) {
            log.warn("No instances found for activity service: {}", serviceName);
            return StringUtils.hasText(baseUrl) ? baseUrl : null;
        }
        return instances.get(0).getUri().toString();
    }

    private List<String> resolveCandidateBaseUrls() {
        List<String> urls = new ArrayList<>();

        if (discoveryClient != null && StringUtils.hasText(serviceName)) {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            if (instances != null) {
                for (ServiceInstance instance : instances) {
                    if (instance == null || instance.getUri() == null) continue;
                    String uri = instance.getUri().toString();
                    if (StringUtils.hasText(uri) && !urls.contains(uri)) {
                        urls.add(uri);
                    }
                }
            }
        }

        // Prefer Nacos instances, but allow explicit fallback base URL.
        if (StringUtils.hasText(baseUrl) && !urls.contains(baseUrl)) {
            urls.add(baseUrl);
        }

        // Add loopback variants to make local dev resilient (e.g. Nacos registers LAN IP).
        List<String> loopbacks = new ArrayList<>();
        for (String u : urls) {
            try {
                URI uri = URI.create(u);
                String host = uri.getHost();
                int port = uri.getPort();
                if (!StringUtils.hasText(host) || port <= 0 || isLoopbackHost(host)) continue;
                String loopback = uri.getScheme() + "://127.0.0.1:" + port;
                if (!urls.contains(loopback) && !loopbacks.contains(loopback)) {
                    loopbacks.add(loopback);
                }
            } catch (Exception ignored) {
            }
        }
        urls.addAll(loopbacks);

        return urls;
    }

    private boolean isLoopbackHost(String host) {
        String normalized = host.trim().toLowerCase();
        return "localhost".equals(normalized) || "127.0.0.1".equals(normalized) || "::1".equals(normalized);
    }

    private String joinBaseUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        return baseUrl + path;
    }

    private String safeSnippet(String value, int maxLen) {
        if (!StringUtils.hasText(value)) return "";
        String trimmed = value.trim().replaceAll("\\s+", " ");
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen) + "...";
    }
}
