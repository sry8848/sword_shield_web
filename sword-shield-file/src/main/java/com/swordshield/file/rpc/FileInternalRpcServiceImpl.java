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

/**
 * 文件内部RPC服务实现类
 * 提供文件上传和预签名链接生成的内部RPC接口
 */
@DubboService
public class FileInternalRpcServiceImpl implements FileInternalRpcService {
    private final FileService fileService;

    public FileInternalRpcServiceImpl(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 上传文件并生成预签名链接
     * 该方法将文件内容上传到存储系统，并返回一个有时效性的预签名下载链接
     *
     * @param bytes 文件内容的字节数组
     * @param filename 文件原始名称
     * @param contentType 文件MIME类型
     * @param authorization 认证授权信息
     * @param expireSeconds 预签名链接过期时间（秒），如果小于等于0则默认为300秒
     * @return 预签名的文件下载链接
     * @throws IllegalArgumentException 当文件内容为空或缺少授权信息时抛出
     * @throws IllegalStateException 当上传或生成预签名链接失败时抛出
     */
    @Override
    public String uploadAndPresign(byte[] bytes, String filename, String contentType, String authorization, int expireSeconds) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("文件内容不能为空");
        }
        if (!StringUtils.hasText(authorization)) {
            throw new IllegalArgumentException("缺少 Authorization");
        }

        // 设置默认过期时间，如果传入的过期时间小于等于0则使用默认值300秒
        int ttl = expireSeconds > 0 ? expireSeconds : 300;
        // 设置默认内容类型，如果未指定则使用application/octet-stream
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

    /**
     * 生成唯一的对象存储名称
     * 使用UUID生成唯一标识，并保留原始文件的扩展名
     *
     * @param filename 原始文件名
     * @return 生成的对象存储名称
     */
    private String generateObjectName(String filename) {
        String extension = "";
        if (StringUtils.hasText(filename) && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }
}

