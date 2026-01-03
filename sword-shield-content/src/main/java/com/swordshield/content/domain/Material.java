package com.swordshield.content.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 演讲/资料素材（Domain Entity）。
 *
 * <p>记录上传到对象存储（MinIO）的素材元数据（对象 Key、类型、大小等），用于后续下载或关联展示。
 */
@Entity
@Table(name = "material")
public class Material {
    /** 素材主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 素材标题。 */
    private String title;

    /** 讲师/分享者（可为空）。 */
    private String speaker;

    /** 对象存储（MinIO）中的对象 Key（路径/文件名）。 */
    private String objectName;

    /** 文件 MIME 类型（可为空）。 */
    private String contentType;

    /** 文件大小（字节）。 */
    private long size;

    /** 创建时间。 */
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }
    public String getObjectName() { return objectName; }
    public void setObjectName(String objectName) { this.objectName = objectName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
