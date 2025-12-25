package com.swordshield.content.service;

import com.swordshield.api.file.rpc.FileInternalRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FileServiceClient {
    private static final Logger log = LoggerFactory.getLogger(FileServiceClient.class);

    private final String serviceAuthorization;

    @DubboReference(check = false)
    private FileInternalRpcService fileInternalRpcService;

    public FileServiceClient(@Value("${file.service.authorization:}") String serviceAuthorization) {
        this.serviceAuthorization = serviceAuthorization == null ? "" : serviceAuthorization.trim();
    }

    public String uploadAndPresign(byte[] bytes, String filename, String authorization, int expireSeconds) {
        String authHeader = resolveAuthorization(authorization);
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }
        try {
            String contentType = guessContentType(filename);
            String url = fileInternalRpcService.uploadAndPresign(bytes, filename, contentType, authHeader, expireSeconds);
            return StringUtils.hasText(url) ? url : null;
        } catch (Exception e) {
            log.warn("File upload/presign via RPC failed: {}", e.getMessage());
            return null;
        }
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

    private String guessContentType(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return null;
    }
}
