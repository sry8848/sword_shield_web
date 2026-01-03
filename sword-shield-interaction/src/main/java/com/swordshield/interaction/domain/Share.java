package com.swordshield.interaction.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 分享记录（Domain Entity）。
 *
 * <p>表示某用户将某个目标对象分享至某个渠道（微信/微博/链接等）的行为记录。
 */
@Entity
@Table(name = "share_record")
public class Share {
    /** 分享记录主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 分享用户 ID。 */
    private Long userId;

    /** 目标类型（例如 article/activity）。 */
    private String targetType;

    /** 目标对象 ID（例如文章 ID、活动 ID）。 */
    private Long targetId;

    /** 分享渠道（例如 wechat/weibo/link）。 */
    private String channel;

    /** 分享时间。 */
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
