package com.swordshield.user.controller;

import com.swordshield.common.Response;
import com.swordshield.user.dto.LoginRequest;
import com.swordshield.user.dto.LoginResponse;
import com.swordshield.user.dto.RegisterRequest;
import com.swordshield.user.dto.UpdateUserProfileRequest;
import com.swordshield.user.dto.UserProfileDto;
import com.swordshield.user.service.UserService;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户", description = "用户注册、登录及资料管理")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建账号，需提供用户名、密码和确认密码")
    public Response<Object> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "注册信息，包含 username / password / confirmPassword",
                    required = true,
                    content = @Content(schema = @Schema(example = "{\"username\":\"demo\",\"password\":\"123456\",\"confirmPassword\":\"123456\"}")))
            @RequestBody RegisterRequest body) {
        service.register(body.getUsername(), body.getPassword(), body.getConfirmPassword());
        return Response.ok(null);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "返回 JWT 令牌与用户信息标识")
    public Response<LoginResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "登录信息，包含 username / password",
                    required = true,
                    content = @Content(schema = @Schema(example = "{\"username\":\"demo\",\"password\":\"123456\"}")))
            @RequestBody LoginRequest body) {
        return Response.ok(service.login(body.getUsername(), body.getPassword()));
    }

    @GetMapping("/profile")
    @Operation(summary = "查询用户资料", description = "根据用户 ID 获取基本信息")
    public Response<UserProfileDto> profile(@Parameter(description = "用户ID", required = true) @RequestParam("id") Long id) {
        return Response.ok(service.getProfile(id));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新用户资料", description = "修改昵称、性别、联系方式和头像")
    public Response<UserProfileDto> update(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户资料", required = true) @RequestBody UpdateUserProfileRequest body) {
        return Response.ok(service.updateProfile(body));
    }
}
