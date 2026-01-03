package com.swordshield.interaction.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 点赞记录（Domain Entity）。
 *
 * <p>表示某用户对某个目标对象（文章/活动等）的点赞行为；由唯一约束保证不会重复点赞。
 */
@Entity
@Table(name = "like_record", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "target_type", "target_id"})
})
public class Like {
    /** 点赞记录主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 点赞用户 ID。 */
    @Column(name = "user_id")
    private Long userId;

    /** 目标类型（例如 article/activity）。 */
    @Column(name = "target_type")
    private String targetType;

    /** 目标对象 ID（例如文章 ID、活动 ID）。 */
    @Column(name = "target_id")
    private Long targetId;

    /** 点赞时间。 */
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
