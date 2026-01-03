package com.swordshield.user.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 用户账号（Domain Entity）。
 *
 * <p>表示用户基础信息与登录凭据（密码哈希），通常与数据库表 {@code user} 对应。
 */
@Entity
@Table(name = "user")
public class User {
    /** 用户主键 ID。 */
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

    /** 性别（可为空，具体取值由业务约定）。 */
    private String gender;

    /** 手机号（可为空）。 */
    private String phone;

    /** 邮箱（可为空）。 */
    private String email;

    /** 头像 URL（可为空/可为默认头像）。 */
    private String avatarUrl;

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
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
