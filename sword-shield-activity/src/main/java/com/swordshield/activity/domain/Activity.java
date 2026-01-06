package com.swordshield.activity.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 活动领域对象（Domain Entity）。
 *
 * <p>表示一个可展示/可报名的活动，通常与数据库表 {@code activity} 对应，包含活动的基础信息与状态。
 */
@Entity
@Table(name = "activity")
public class Activity {
    /** 活动主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 活动标题（必填）。 */
    @Column(nullable = false)
    private String title;

    /** 活动摘要/简介（用于列表展示，可为空）。 */
    private String summary;

    /** 活动封面图片 URL（可为空）。 */
    private String coverUrl;

    /** 活动开始时间（可为空）。 */
    private Instant startTime;

    /** 活动结束时间（可为空）。 */
    private Instant endTime;

    /** 活动地点/地址（可为空）。 */
    private String location;

    /** 演讲嘉宾/讲师信息（可为空）。 */
    private String speaker;

    /** 直播链接（可为空）。 */
    private String liveUrl;

    /** 回放链接（可为空）。 */
    private String replayUrl;

    /** 最大可报名人数；为空表示不限制。 */
    private Integer maxRegistrations;

    /** 当前报名人数（用于展示与名额限制）。 */
    private Integer currentRegistrations;

    /** 浏览量（用于热度计算与展示，可为空）。 */
    private Integer views;

    /** 热度/人气值（用于排序与展示）。 */
    private Integer heat;

    /** 状态：0-下线，1-发布。 */
    private Integer status;

    /** 创建时间（通常由服务端写入）。 */
    private Instant createdAt;

    /** 更新时间（通常由服务端写入）。 */
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }
    public String getLiveUrl() { return liveUrl; }
    public void setLiveUrl(String liveUrl) { this.liveUrl = liveUrl; }
    public String getReplayUrl() { return replayUrl; }
    public void setReplayUrl(String replayUrl) { this.replayUrl = replayUrl; }
    public Integer getMaxRegistrations() { return maxRegistrations; }
    public void setMaxRegistrations(Integer maxRegistrations) { this.maxRegistrations = maxRegistrations; }
    public Integer getCurrentRegistrations() { return currentRegistrations; }
    public void setCurrentRegistrations(Integer currentRegistrations) { this.currentRegistrations = currentRegistrations; }
    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }
    public Integer getHeat() { return heat; }
    public void setHeat(Integer heat) { this.heat = heat; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

