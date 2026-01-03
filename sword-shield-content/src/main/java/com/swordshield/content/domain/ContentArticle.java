package com.swordshield.content.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 内容文章（Domain Entity）。
 *
 * <p>表示站内文章/资讯内容，通常与数据库表 {@code content_article} 对应，包含正文、封面、分类、作者、发布状态等信息。
 */
@Entity
@Table(name = "content_article")
public class ContentArticle {
    /** 文章主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 文章标题。 */
    private String title;

    /** 文章摘要/简介（用于列表展示）。 */
    private String summary;

    /** 文章正文内容（LONGTEXT）。 */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String body;

    /** 封面图片 URL（可为空）。 */
    private String coverUrl;

    /** 文章分类/频道（例如 通用/UGC/公告 等）。 */
    private String category;

    /** 作者名称/署名（可为空）。 */
    private String author;

    /** 阅读量/浏览次数。 */
    private int views;

    /** 直播链接（可为空）。 */
    private String liveUrl;

    /** 回放链接（可为空）。 */
    private String replayUrl;

    /** 状态：0-草稿（draft），1-发布（published）。 */
    private int status;

    /** 发布时间（发布时写入；草稿状态通常为空）。 */
    private Instant publishedAt;

    /** 创建时间（由数据库写入/维护）。 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    /** 更新时间（由数据库写入/维护）。 */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }
    public String getLiveUrl() { return liveUrl; }
    public void setLiveUrl(String liveUrl) { this.liveUrl = liveUrl; }
    public String getReplayUrl() { return replayUrl; }
    public void setReplayUrl(String replayUrl) { this.replayUrl = replayUrl; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
