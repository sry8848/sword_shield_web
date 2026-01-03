package com.swordshield.user.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 用户活动签到记录（Domain Entity）。
 *
 * <p>记录用户对某个活动的签到行为；同一用户对同一活动仅允许签到一次（唯一约束）。
 */
@Entity
@Table(
        name = "activity_checkin",
        uniqueConstraints = { @UniqueConstraint(columnNames = {"user_id", "activity_id"}) }
)
public class ActivityCheckin {
    /** 活动签到记录主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户 ID。 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 活动 ID（对应活动表主键）。 */
    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    /** 签到时间。 */
    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Instant getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(Instant checkedInAt) { this.checkedInAt = checkedInAt; }
}
