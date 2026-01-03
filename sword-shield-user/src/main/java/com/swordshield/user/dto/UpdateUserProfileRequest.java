package com.swordshield.user.dto;

import lombok.Data;

/**
 * 更新用户资料请求 DTO。
 *
 * <p>用于修改用户昵称、联系方式、头像等可编辑字段。
 */
@Data
public class UpdateUserProfileRequest {
    /** 用户 ID（必填）。 */
    private Long id;

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
}
