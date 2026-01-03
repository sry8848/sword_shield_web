package com.swordshield.activity.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 活动报名记录（Domain Entity）。
 *
 * <p>用于关联用户与活动的报名关系，同一用户对同一活动仅允许报名一次（由唯一约束保证）。
 */
@Entity
@Table(name = "activity_registration", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"activity_id", "user_id"})
})
public class ActivityRegistration {
    /** 报名记录主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 活动 ID（对应 {@code activity.id}）。 */
    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    /** 用户 ID（对应 {@code user.id}）。 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 报名渠道（例如 web/app/onsite）。 */
    private String channel;

    /** 报名时间。 */
    private Instant registeredAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
}
