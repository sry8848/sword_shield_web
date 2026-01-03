package com.swordshield.user.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 活动引用（Domain Read Model）。
 *
 * <p>用户服务在活动签到等用例中需要校验活动是否存在以及时间窗口。
 * 该类通过只读取 {@code activity} 表的少量字段，作为用户服务侧的「活动信息投影/引用」。
 */
@Entity
@Table(name = "activity")
public class ActivityRef {
    /** 活动 ID（对应活动表主键）。 */
    @Id
    private Long id;

    /** 活动标题（用于提示/展示）。 */
    private String title;

    /** 活动开始时间（用于签到时间窗口校验）。 */
    private Instant startTime;

    /** 活动结束时间（用于签到时间窗口校验）。 */
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
