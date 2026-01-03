package com.swordshield.user.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 用户积分汇总（Domain Entity）。
 *
 * <p>记录用户积分的汇总视图（总积分/可用/已用），与积分流水 {@link com.swordshield.user.domain.PointsTxn} 配合使用。
 */
@Entity
@Table(name = "user_points")
public class UserPoints {
    /** 积分汇总记录主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** 用户 ID（唯一，一名用户一条汇总记录）。 */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    /** 累计获得的总积分。 */
    @Column(nullable = false)
    private Integer total = 0;
    
    /** 当前可用积分（可用于兑换/消耗）。 */
    @Column(nullable = false)
    private Integer available = 0;
    
    /** 已使用/已消耗的积分。 */
    @Column(nullable = false)
    private Integer used = 0;
    
    /** 创建时间。 */
    @Column(name = "created_at")
    private Instant createdAt;
    
    /** 更新时间。 */
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
    
    public Integer getAvailable() { return available; }
    public void setAvailable(Integer available) { this.available = available; }
    
    public Integer getUsed() { return used; }
    public void setUsed(Integer used) { this.used = used; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
