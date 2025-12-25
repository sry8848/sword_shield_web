package com.swordshield.file.rpc;

import com.swordshield.api.file.AssetFileDto;
import com.swordshield.api.file.rpc.FileInternalRpcService;
import com.swordshield.common.AuthUtils;
import com.swordshield.file.service.FileService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@DubboService
public class FileInternalRpcServiceImpl implements FileInternalRpcService {
    private final FileService fileService;

    public FileInternalRpcServiceImpl(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public String uploadAndPresign(byte[] bytes, String filename, String contentType, String authorization, int expireSeconds) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("文件内容不能为空");
        }
        if (!StringUtils.hasText(authorization)) {
            throw new IllegalArgumentException("缺少 Authorization");
        }

        int ttl = expireSeconds > 0 ? expireSeconds : 300;
        String resolvedContentType = StringUtils.hasText(contentType) ? contentType : "application/octet-stream";

        Long ownerId = AuthUtils.requireUserIdAllowAdmin(null, authorization);
        String objectName = generateObjectName(filename);

        try (InputStream in = new ByteArrayInputStream(bytes)) {
            AssetFileDto meta = fileService.putObject(
                    objectName,
                    filename,
                    resolvedContentType,
                    bytes.length,
                    in,
                    ownerId
            );
            return fileService.presignedGet(meta.getObjectName(), ttl);
        } catch (Exception e) {
            throw new IllegalStateException("上传或生成预签名链接失败: " + e.getMessage(), e);
        }
    }

    private String generateObjectName(String filename) {
        String extension = "";
        if (StringUtils.hasText(filename) && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }
}

