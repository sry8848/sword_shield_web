package com.swordshield.admin.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 后台管理员账号（Domain Entity）。
 *
 * <p>用于管理端登录与权限控制，通常与数据库表 {@code admin_user} 对应。
 */
@Entity
@Table(name = "admin_user")
public class AdminUser {
    /** 管理员主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** 登录用户名（唯一）。 */
    @Column(unique = true, nullable = false)
    private String username;
    
    /** 密码哈希（BCrypt）。 */
    @Column(name = "password", nullable = false)
    private String passwordHash;
    
    /** 显示昵称（可为空）。 */
    private String nickname;

    /** 账号状态：0-禁用，1-正常。 */
    private Integer status;

    /** 创建时间。 */
    private Instant createdAt;

    /** 更新时间。 */
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
