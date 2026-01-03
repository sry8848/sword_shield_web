package com.swordshield.user.dto;

import lombok.Data;

/**
 * 用户注册请求 DTO。
 *
 * <p>用于注册新用户，包含两次密码输入以做一致性校验。
 */
@Data
public class RegisterRequest {
    /** 用户名（用于登录）。 */
    private String username;

    /** 密码（明文）。 */
    private String password;

    /** 确认密码（需与 {@code password} 一致）。 */
    private String confirmPassword;
}
