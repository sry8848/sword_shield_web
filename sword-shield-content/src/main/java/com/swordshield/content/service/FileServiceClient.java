package com.swordshield.content.service;

import com.swordshield.common.Response;
import com.swordshield.content.client.FileServiceDirectClient;
import com.swordshield.content.client.FileServiceDiscoveryClient;
import com.swordshield.content.service.dto.AssetFileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class FileServiceClient {
    private static final Logger log = LoggerFactory.getLogger(FileServiceClient.class);

    private final FileServiceDiscoveryClient discoveryClient;
    private final ObjectProvider<FileServiceDirectClient> directClientProvider;
    private final String serviceAuthorization;

    public FileServiceClient(FileServiceDiscoveryClient discoveryClient,
                             ObjectProvider<FileServiceDirectClient> directClientProvider,
                             @Value("${file.service.authorization:}") String serviceAuthorization) {
        this.discoveryClient = discoveryClient;
        this.directClientProvider = directClientProvider;
        this.serviceAuthorization = serviceAuthorization == null ? "" : serviceAuthorization.trim();
    }

    public String uploadAndPresign(byte[] bytes, String filename, String authorization, int expireSeconds) {
        String authHeader = resolveAuthorization(authorization);
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }

        Resource fileResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        for (com.swordshield.content.client.FileServiceApi client : candidateClients()) {
            String url = uploadAndPresignOnce(client, fileResource, authHeader, expireSeconds);
            if (StringUtils.hasText(url)) {
                return url;
            }
        }
        return null;
    }

    private String uploadAndPresignOnce(com.swordshield.content.client.FileServiceApi client,
                                        Resource fileResource,
                                        String authHeader,
                                        int expireSeconds) {
        Response<AssetFileDto> uploadBody;
        try {
            uploadBody = client.upload(authHeader, fileResource);
        } catch (Exception e) {
            log.warn("File upload failed: {}", e.getMessage());
            return null;
        }

        if (uploadBody == null) {
            log.warn("File upload returned empty body");
            return null;
        }
        if (uploadBody.getCode() != 0) {
            log.warn("File upload returned error: code={} message={}", uploadBody.getCode(), uploadBody.getMessage());
            return null;
        }
        if (uploadBody.getData() == null || !StringUtils.hasText(uploadBody.getData().getObjectName())) {
            log.warn("File upload returned empty objectName");
            return null;
        }
        String objectName = uploadBody.getData().getObjectName();

        Response<String> presignBody;
        try {
            presignBody = client.presigned(objectName, expireSeconds);
        } catch (Exception e) {
            log.warn("Presign request failed: {}", e.getMessage());
            return null;
        }

        if (presignBody == null) {
            log.warn("Presign request returned empty body: objectName={}", objectName);
            return null;
        }
        if (presignBody.getCode() != 0) {
            log.warn("Presign request returned error: objectName={} code={} message={}",
                    objectName, presignBody.getCode(), presignBody.getMessage());
            return null;
        }
        return presignBody.getData();
    }

    private String resolveAuthorization(String authorization) {
        if (StringUtils.hasText(authorization)) {
            return authorization;
        }
        return serviceAuthorization.isBlank() ? null : serviceAuthorization;
    }

    public boolean hasServiceAuthorization() {
        return !serviceAuthorization.isBlank();
    }

    private List<com.swordshield.content.client.FileServiceApi> candidateClients() {
        List<com.swordshield.content.client.FileServiceApi> clients = new ArrayList<>();
        FileServiceDirectClient direct = directClientProvider.getIfAvailable();
        if (direct != null) {
            clients.add(direct);
        }
        clients.add(discoveryClient);
        return clients;
    }
}
