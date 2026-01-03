package com.swordshield.user.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 积分流水（Domain Entity）。
 *
 * <p>记录一次积分变动的明细：变动类型、分值、说明与时间等，用于积分明细查询与审计。
 */
@Entity
@Table(name = "points_txn")
public class PointsTxn {
    /** 积分流水主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户 ID。 */
    private Long userId;

    /** 变动类型（例如 daily_checkin / activity_checkin）。 */
    private String type;

    /** 变动积分值（正数表示获得，负数表示扣减）。 */
    private Integer points;

    /** 备注说明（可为空）。 */
    private String note;

    /** 发生时间。 */
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
