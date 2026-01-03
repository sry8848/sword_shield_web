package com.swordshield.interaction.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 评论记录（Domain Entity）。
 *
 * <p>表示某用户对某个目标对象（文章/活动等）的评论内容。
 */
@Entity
@Table(name = "comment")
public class Comment {
    /** 评论主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 评论用户 ID。 */
    private Long userId;

    /** 目标类型（例如 article/activity）。 */
    private String targetType;

    /** 目标对象 ID（例如文章 ID、活动 ID）。 */
    private Long targetId;

    /** 评论内容（最大 1000 字符）。 */
    @Column(length = 1000)
    private String content;

    /** 评论创建时间。 */
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
