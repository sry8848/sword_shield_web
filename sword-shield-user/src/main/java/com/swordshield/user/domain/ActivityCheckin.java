package com.swordshield.user.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "activity_checkin",
        uniqueConstraints = { @UniqueConstraint(columnNames = {"user_id", "activity_id"}) }
)
public class ActivityCheckin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

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

