package com.swordshield.user.dto;

import lombok.Data;

/**
 * 用户登录请求 DTO。
 *
 * <p>携带用户名与密码，用于换取登录 Token。
 */
@Data
public class LoginRequest {
    /** 用户名。 */
    private String username;

    /** 密码（明文，仅用于登录校验）。 */
    private String password;
}
