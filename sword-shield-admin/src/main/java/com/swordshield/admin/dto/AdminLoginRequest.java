package com.swordshield.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员登录请求 DTO。
 *
 * <p>对应后台登录接口的请求体：用户名与明文密码（服务端会进行校验并生成 Token）。
 */
@Data
public class AdminLoginRequest {
    /** 管理员用户名。 */
    @NotBlank(message = "username 不能为空")
    private String username;

    /** 管理员密码（明文，仅用于登录校验）。 */
    @NotBlank(message = "password 不能为空")
    private String password;
}
