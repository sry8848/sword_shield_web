package com.swordshield.user.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * 用户每日签到记录（Domain Entity）。
 *
 * <p>用于记录用户在某一天是否完成签到，由 {@code (user_id, checkin_date)} 唯一约束保证不可重复签到。
 */
@Entity
@Table(name = "user_checkin", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "checkin_date"})
})
public class Checkin {
    /** 每日签到记录主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户 ID。 */
    @Column(name = "user_id")
    private Long userId;

    /** 签到日期（LocalDate，例如 2026-01-03）。 */
    @Column(name = "checkin_date")
    private LocalDate date;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
