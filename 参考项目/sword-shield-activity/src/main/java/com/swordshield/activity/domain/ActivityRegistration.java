package com.swordshield.activity.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "activity_registration", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"activity_id", "user_id"})
})
public class ActivityRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "activity_id", nullable = false)
    private Long activityId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    private String channel; // e.g. web/app/onsite
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

