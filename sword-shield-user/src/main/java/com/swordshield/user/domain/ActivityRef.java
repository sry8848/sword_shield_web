package com.swordshield.user.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "activity")
public class ActivityRef {
    @Id
    private Long id;
    private String title;
    private Instant startTime;
    private Instant endTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
}

