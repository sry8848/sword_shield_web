package com.swordshield.api.file;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 文件元数据 DTO（跨服务契约）。
 *
 * <p>用于文件服务对外/服务间返回文件元数据，通常对应领域对象：{@link com.swordshield.file.domain.AssetFile}。
 */
@Data
public class AssetFileDto implements Serializable {
    /** 文件记录 ID。 */
    private Long id;

    /** 对象存储中的对象 Key（路径/文件名）。 */
    private String objectName;

    /** 存储桶名称。 */
    private String bucket;

    /** 文件 MIME 类型（可为空）。 */
    private String contentType;

    /** 文件大小（字节）。 */
    private Long size;

    /** 文件归属用户 ID（可为空）。 */
    private Long ownerId;

    /** 创建时间。 */
    private Instant createdAt;

    /** 原始文件名（上传时的文件名）。 */
    private String originalName;
}
