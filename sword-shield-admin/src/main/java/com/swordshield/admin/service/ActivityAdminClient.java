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

import java.util.List;
import java.util.Map;

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
        String base = resolveBaseUrl();
        if (!StringUtils.hasText(base)) {
            return Response.error(502, "Activity service unavailable");
        }
        String url = joinBaseUrl(base, "/api/activity/admin/activities");
        return exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders(authorization)),
                new ParameterizedTypeReference<Response<List<ActivityDto>>>() {});
    }

    public Response<Map<String, Object>> list(String authorization, int page, int size) {
        String base = resolveBaseUrl();
        if (!StringUtils.hasText(base)) {
            return Response.error(502, "Activity service unavailable");
        }
        String url = UriComponentsBuilder.fromHttpUrl(joinBaseUrl(base, "/api/activity/admin/activities/list"))
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();
        return exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders(authorization)),
                new ParameterizedTypeReference<Response<Map<String, Object>>>() {});
    }

    public Response<ActivityDto> getById(String authorization, Long id) {
        String base = resolveBaseUrl();
        if (!StringUtils.hasText(base)) {
            return Response.error(502, "Activity service unavailable");
        }
        String url = joinBaseUrl(base, "/api/activity/admin/activities/" + id);
        return exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders(authorization)),
                new ParameterizedTypeReference<Response<ActivityDto>>() {});
    }

    public Response<ActivityDto> create(String authorization, ActivityDto body) {
        String base = resolveBaseUrl();
        if (!StringUtils.hasText(base)) {
            return Response.error(502, "Activity service unavailable");
        }
        String url = joinBaseUrl(base, "/api/activity/admin/activities");
        return exchange(url, HttpMethod.POST, new HttpEntity<>(body, authHeaders(authorization)),
                new ParameterizedTypeReference<Response<ActivityDto>>() {});
    }

    public Response<ActivityDto> update(String authorization, Long id, ActivityDto body) {
        String base = resolveBaseUrl();
        if (!StringUtils.hasText(base)) {
            return Response.error(502, "Activity service unavailable");
        }
        String url = joinBaseUrl(base, "/api/activity/admin/activities/" + id);
        return exchange(url, HttpMethod.PUT, new HttpEntity<>(body, authHeaders(authorization)),
                new ParameterizedTypeReference<Response<ActivityDto>>() {});
    }

    public Response<Object> delete(String authorization, Long id) {
        String base = resolveBaseUrl();
        if (!StringUtils.hasText(base)) {
            return Response.error(502, "Activity service unavailable");
        }
        String url = joinBaseUrl(base, "/api/activity/admin/activities/" + id);
        return exchange(url, HttpMethod.DELETE, new HttpEntity<>(authHeaders(authorization)),
                new ParameterizedTypeReference<Response<Object>>() {});
    }

    private HttpHeaders authHeaders(String authorization) {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.hasText(authorization)) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
        return headers;
    }

    private <T> Response<T> exchange(String url,
                                    HttpMethod method,
                                    HttpEntity<?> request,
                                    ParameterizedTypeReference<Response<T>> typeRef) {
        try {
            ResponseEntity<Response<T>> response = restTemplate.exchange(url, method, request, typeRef);
            return response.getBody();
        } catch (RestClientResponseException e) {
            log.warn("Activity admin call failed: url={} status={} body={}", url, e.getRawStatusCode(), safeSnippet(e.getResponseBodyAsString(), 500));
            return Response.error(e.getRawStatusCode(), "Activity service error");
        } catch (Exception e) {
            log.warn("Activity admin call failed: url={} error={}", url, e.getMessage());
            return Response.error(502, "Activity service unavailable");
        }
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
