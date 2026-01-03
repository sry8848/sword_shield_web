package com.swordshield.file.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 文件元数据记录（Domain Entity）。
 *
 * <p>表示一个已写入对象存储（MinIO）的文件条目，保存对象 Key、桶、类型、大小、归属等信息。
 */
@Entity
@Table(name = "asset_file")
public class AssetFile {
    /** 文件记录主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对象存储中的对象 Key（路径/文件名）。 */
    private String objectName;

    /** 存储桶名称。 */
    private String bucket;

    /** 文件 MIME 类型（可为空）。 */
    private String contentType;

    /** 文件大小（字节）。 */
    private Long size;

    /** 文件归属用户 ID（可为空，系统文件可能无归属）。 */
    private Long ownerId;

    /** 创建时间。 */
    private Instant createdAt;

    /** 原始文件名（上传时的文件名）。 */
    private String originalName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getObjectName() { return objectName; }
    public void setObjectName(String objectName) { this.objectName = objectName; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
}
