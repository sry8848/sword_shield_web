package com.swordshield.admin.controller;

import com.swordshield.admin.service.AdminAuthService;
import com.swordshield.common.Response;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "管理员认证", description = "后台登录与令牌发放")
public class AdminAuthController {
    private final AdminAuthService authService;

    public AdminAuthController(AdminAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "管理员用户名/密码登录，返回 JWT 令牌")
    public Response<Map<String, String>> login(@RequestBody Map<String, String> body) {
        System.out.printf("Login Request: %s\n", body);
        Map<String, String> result = authService.login(body.get("username"), body.get("password"));
        return Response.ok(result);
    }
}
