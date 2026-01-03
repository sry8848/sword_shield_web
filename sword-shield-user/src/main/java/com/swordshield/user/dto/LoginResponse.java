package com.swordshield.user.dto;

import lombok.Data;

/**
 * 用户登录响应 DTO。
 *
 * <p>登录成功后返回的访问 Token 与用户 ID。
 */
@Data
public class LoginResponse {
    /** 登录成功后的访问 Token（JWT）。 */
    private String token;

    /** 用户 ID（字符串形式）。 */
    private String userId;
}
