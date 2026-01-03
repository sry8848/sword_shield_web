package com.swordshield.content.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 媒体资源（Domain Entity）。
 *
 * <p>用于管理图片/视频等媒体资源，包含对象存储 Key、封面、关联文章等元数据。
 */
@Entity
@Table(name = "media_asset")
public class MediaAsset {
    /** 媒体资源主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 资源标题。 */
    private String title;

    /** 资源摘要/说明（可为空）。 */
    @Column(length = 1000)
    private String summary;

    /** 资源类型：image | video。 */
    private String type;

    /** 对象存储（MinIO）中的对象 Key（路径/文件名）。 */
    private String objectName;

    /** 封面图片 URL（可为空）。 */
    private String coverUrl;

    /** 关联文章 ID（可为空；用于将媒体挂载到某篇文章）。 */
    private Long articleId;

    /** 创建时间。 */
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getObjectName() { return objectName; }
    public void setObjectName(String objectName) { this.objectName = objectName; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
