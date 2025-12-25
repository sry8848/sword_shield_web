package com.swordshield.content.service;

import com.swordshield.common.Response;
import com.swordshield.file.domain.AssetFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FileServiceClient {
    private static final Logger log = LoggerFactory.getLogger(FileServiceClient.class);
    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final String baseUrl;
    private final String serviceName;
    private final String serviceAuthorization;

    public FileServiceClient(RestTemplateBuilder builder,
                             DiscoveryClient discoveryClient,
                             @Value("${file.service.base-url:}") String baseUrl,
                             @Value("${file.service.name:sword-shield-file}") String serviceName,
                             @Value("${file.service.authorization:}") String serviceAuthorization) {
        this.restTemplate = builder.build();
        this.discoveryClient = discoveryClient;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.serviceName = serviceName == null ? "" : serviceName.trim();
        this.serviceAuthorization = serviceAuthorization == null ? "" : serviceAuthorization.trim();
    }

    public String uploadAndPresign(byte[] bytes, String filename, String authorization, int expireSeconds) {
        String authHeader = resolveAuthorization(authorization);
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        String serviceBaseUrl = resolveBaseUrl();
        if (!StringUtils.hasText(serviceBaseUrl)) {
            return null;
        }
        java.util.List<String> baseUrls = buildCandidateBaseUrls(serviceBaseUrl);
        for (String baseUrlCandidate : baseUrls) {
            String url = uploadAndPresignOnce(baseUrlCandidate, bytes, filename, authHeader, expireSeconds);
            if (StringUtils.hasText(url)) {
                return url;
            }
        }
        return null;
    }

    private String uploadAndPresignOnce(String baseUrlCandidate,
                                        byte[] bytes,
                                        String filename,
                                        String authHeader,
                                        int expireSeconds) {
        log.debug("Using file service base url: {}", baseUrlCandidate);
        String uploadUrl = joinBaseUrl(baseUrlCandidate, "/api/file/upload");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Response<AssetFile>> uploadResponse;
        try {
            uploadResponse = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Response<AssetFile>>() {}
            );
        } catch (RestClientResponseException e) {
            log.warn("File upload failed: baseUrl={} status={} message={}", baseUrlCandidate, e.getRawStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("File upload failed: baseUrl={} error={}", baseUrlCandidate, e.getMessage());
            return null;
        }
        Response<AssetFile> uploadBody = uploadResponse.getBody();
        if (uploadBody == null || uploadBody.getCode() != 0 || uploadBody.getData() == null) {
            return null;
        }
        String objectName = uploadBody.getData().getObjectName();
        if (objectName == null || objectName.isBlank()) {
            return null;
        }

        String presignedUrl = UriComponentsBuilder.fromHttpUrl(joinBaseUrl(baseUrlCandidate, "/api/file/presigned"))
                .queryParam("objectName", objectName)
                .queryParam("expireSeconds", expireSeconds)
                .toUriString();

        ResponseEntity<Response<String>> presignResponse;
        try {
            presignResponse = restTemplate.exchange(
                    presignedUrl,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<Response<String>>() {}
            );
        } catch (RestClientResponseException e) {
            log.warn("Presign request failed: baseUrl={} status={} message={}", baseUrlCandidate, e.getRawStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Presign request failed: baseUrl={} error={}", baseUrlCandidate, e.getMessage());
            return null;
        }
        Response<String> presignBody = presignResponse.getBody();
        if (presignBody == null || presignBody.getCode() != 0) {
            return null;
        }
        return presignBody.getData();
    }

    private java.util.List<String> buildCandidateBaseUrls(String resolvedBaseUrl) {
        java.util.List<String> urls = new java.util.ArrayList<>();
        urls.add(resolvedBaseUrl);
        try {
            java.net.URI uri = java.net.URI.create(resolvedBaseUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            if (!StringUtils.hasText(host) || port <= 0) {
                return urls;
            }
            boolean isExplicitBaseUrl = StringUtils.hasText(baseUrl);
            if (!isExplicitBaseUrl && !isLoopbackHost(host)) {
                String loopback = uri.getScheme() + "://127.0.0.1:" + port;
                if (!urls.contains(loopback)) {
                    urls.add(loopback);
                }
            }
        } catch (Exception ignored) {
        }
        return urls;
    }

    private boolean isLoopbackHost(String host) {
        String normalized = host.trim().toLowerCase();
        return "localhost".equals(normalized) || "127.0.0.1".equals(normalized) || "::1".equals(normalized);
    }

    private String resolveAuthorization(String authorization) {
        if (authorization != null && !authorization.isBlank()) {
            return authorization;
        }
        return serviceAuthorization.isBlank() ? null : serviceAuthorization;
    }

    public boolean hasServiceAuthorization() {
        return !serviceAuthorization.isBlank();
    }

    private String resolveBaseUrl() {
        if (StringUtils.hasText(baseUrl)) {
            return baseUrl;
        }
        if (discoveryClient == null || !StringUtils.hasText(serviceName)) {
            return null;
        }
        java.util.List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances == null || instances.isEmpty()) {
            log.warn("No instances found for file service: {}", serviceName);
            return null;
        }
        ServiceInstance instance = instances.get(0);
        String uri = instance.getUri().toString();
        log.debug("Resolved file service instance: service={} host={} port={} uri={}",
                serviceName, instance.getHost(), instance.getPort(), uri);
        return uri;
    }

    private String joinBaseUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        return baseUrl + path;
    }
}
