package com.swordshield.user.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 用户资料 DTO。
 *
 * <p>用于用户资料查询/更新后的返回结构，来源于 {@link com.swordshield.user.domain.User}。
 */
@Data
public class UserProfileDto {
    /** 用户 ID。 */
    private Long id;

    /** 用户名。 */
    private String username;

    /** 昵称（可为空）。 */
    private String nickname;

    /** 性别（可为空）。 */
    private String gender;

    /** 手机号（可为空）。 */
    private String phone;

    /** 邮箱（可为空）。 */
    private String email;

    /** 头像 URL（可为空）。 */
    private String avatarUrl;

    /** 账号状态：0-禁用，1-正常。 */
    private Integer status;

    /** 创建时间。 */
    private Instant createdAt;

    /** 更新时间。 */
    private Instant updatedAt;
}
