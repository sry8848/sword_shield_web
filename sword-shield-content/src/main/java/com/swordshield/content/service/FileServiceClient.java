package com.swordshield.content.service;

import com.swordshield.api.file.rpc.FileInternalRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 文件服务 RPC 客户端（内容服务侧）。
 *
 * <p>用于调用文件服务内部接口上传文件并获取预签名 URL（例如海报生成后上传）。
 * 该类只负责远程调用与鉴权头解析，不承载内容业务规则。
 */
@Service
public class FileServiceClient {
    private static final Logger log = LoggerFactory.getLogger(FileServiceClient.class);

    private final String serviceAuthorization;

    @DubboReference(check = false)
    private FileInternalRpcService fileInternalRpcService;

    /**
     * 构造函数
     *
     * @param serviceAuthorization 服务授权信息，从配置文件中获取
     */
    public FileServiceClient(@Value("${file.service.authorization:}") String serviceAuthorization) {
        this.serviceAuthorization = serviceAuthorization == null ? "" : serviceAuthorization.trim();
    }

    /**
     * 上传文件并获取预签名URL
     *
     * @param bytes 文件字节数组
     * @param filename 文件名
     * @param authorization 授权信息
     * @param expireSeconds 过期时间（秒）
     * @return 预签名URL，失败时返回null
     */
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

    /**
     * 解析授权信息，优先使用传入的authorization，如果为空则使用服务级别的授权
     *
     * @param authorization 传入的授权信息
     * @return 解析后的授权头信息
     */
    private String resolveAuthorization(String authorization) {
        if (StringUtils.hasText(authorization)) {
            return authorization;
        }
        return serviceAuthorization.isBlank() ? null : serviceAuthorization;
    }

    /**
     * 检查是否配置了服务级别的授权信息
     *
     * @return 如果配置了服务授权返回true，否则返回false
     */
    public boolean hasServiceAuthorization() {
        return !serviceAuthorization.isBlank();
    }

    /**
     * 根据文件扩展名猜测内容类型
     *
     * @param filename 文件名
     * @return 对应的MIME类型，无法识别时返回null
     */
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
