package com.swordshield.user.controller;

import com.swordshield.common.Response;
import com.swordshield.user.domain.User;
import com.swordshield.user.repository.UserRepository;
import com.swordshield.user.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
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
    private final UserRepository repo;

    public UserController(UserService service, UserRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建账号，需提供用户名、密码和确认密码")
    public Response<Object> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "注册信息，包含 username / password / confirmPassword",
                    required = true,
                    content = @Content(schema = @Schema(example = "{\"username\":\"demo\",\"password\":\"123456\",\"confirmPassword\":\"123456\"}")))
            @RequestBody Map<String, String> body) {
        service.register(body.get("username"), body.get("password"), body.get("confirmPassword"));
        return Response.ok(null);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "返回 JWT 令牌与用户信息标识")
    public Response<Map<String, String>> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "登录信息，包含 username / password",
                    required = true,
                    content = @Content(schema = @Schema(example = "{\"username\":\"demo\",\"password\":\"123456\"}")))
            @RequestBody Map<String, String> body) {
        Map<String, String> result = service.login(body.get("username"), body.get("password"));
        return Response.ok(result);
    }

    @GetMapping("/profile")
    @Operation(summary = "查询用户资料", description = "根据用户 ID 获取基本信息")
    public Response<User> profile(@Parameter(description = "用户ID", required = true) @RequestParam("id") Long id) {
        User u = repo.findById(id).orElseThrow(() -> 
            new IllegalArgumentException("用户不存在"));
        return Response.ok(u);
    }

    @PutMapping("/profile")
    @Operation(summary = "更新用户资料", description = "修改昵称、性别、联系方式和头像")
    public Response<User> update(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户资料实体", required = true) @RequestBody User body) {
        User u = repo.findById(body.getId()).orElseThrow(() -> 
            new IllegalArgumentException("用户不存在"));
        u.setNickname(body.getNickname());
        u.setGender(body.getGender());
        u.setPhone(body.getPhone());
        u.setEmail(body.getEmail());
        u.setAvatarUrl(body.getAvatarUrl());
        User saved = repo.save(u);
        return Response.ok(saved);
    }
}

