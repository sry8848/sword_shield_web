package com.swordshield.user.controller;

import com.swordshield.common.Response;
import com.swordshield.user.domain.User;
import com.swordshield.user.repository.UserRepository;
import com.swordshield.user.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService service;
    private final UserRepository repo;

    public UserController(UserService service, UserRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    @PostMapping("/register")
    public Response<Object> register(@RequestBody Map<String, String> body) {
        service.register(body.get("username"), body.get("password"), body.get("confirmPassword"));
        return Response.ok(null);
    }

    @PostMapping("/login")
    public Response<Map<String, String>> login(@RequestBody Map<String, String> body) {
        Map<String, String> result = service.login(body.get("username"), body.get("password"));
        return Response.ok(result);
    }

    @GetMapping("/profile")
    public Response<User> profile(@RequestParam("id") Long id) {
        User u = repo.findById(id).orElseThrow(() -> 
            new IllegalArgumentException("用户不存在"));
        return Response.ok(u);
    }

    @PutMapping("/profile")
    public Response<User> update(@RequestBody User body) {
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

