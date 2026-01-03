package com.swordshield.file.service;

import com.swordshield.file.domain.AssetFile;
import com.swordshield.api.file.AssetFileDto;
import com.swordshield.file.repository.AssetFileRepository;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.time.Instant;

/**
 * 文件服务核心应用服务。
 *
 * <p>封装对 MinIO 的写入与预签名 URL 生成，并将对象存储中的文件元数据持久化到
 * {@link com.swordshield.file.domain.AssetFile}。
 *
 * <p>对外/跨服务返回的数据结构使用 {@link com.swordshield.api.file.AssetFileDto}。
 */
@Service
public class FileService {
    private final MinioClient minio;
    private final AssetFileRepository repo;
    private final String bucket;

    public FileService(MinioClient minio, AssetFileRepository repo, @Value("${minio.bucket}") String bucket) {
        this.minio = minio;
        this.repo = repo;
        this.bucket = bucket;
    }

    public AssetFileDto putObject(String objectName, String originalName, String contentType, long size, InputStream stream, Long ownerId) throws Exception {
        ensureBucket();
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .contentType(contentType)
                .stream(stream, size, -1)
                .build();
        minio.putObject(args);
        AssetFile f = new AssetFile();
        f.setBucket(bucket);
        f.setObjectName(objectName);
        f.setOriginalName(originalName);
        f.setContentType(contentType);
        f.setSize(size);
        f.setOwnerId(ownerId);
        f.setCreatedAt(Instant.now());
        return toDto(repo.save(f));
    }

    public String presignedGet(String objectName, int expireSeconds) throws Exception {
        ensureBucket();
        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .method(Method.GET)
                .expiry(expireSeconds)
                .build();
        return minio.getPresignedObjectUrl(args);
    }

    private void ensureBucket() throws Exception {
        boolean exists = minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private AssetFileDto toDto(AssetFile f) {
        AssetFileDto dto = new AssetFileDto();
        dto.setId(f.getId());
        dto.setObjectName(f.getObjectName());
        dto.setBucket(f.getBucket());
        dto.setContentType(f.getContentType());
        dto.setSize(f.getSize());
        dto.setOwnerId(f.getOwnerId());
        dto.setCreatedAt(f.getCreatedAt());
        dto.setOriginalName(f.getOriginalName());
        return dto;
    }
}
