package com.swordshield.admin.controller;

import com.swordshield.admin.service.AdminAuthService;
import com.swordshield.common.Response;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AdminAuthService authService;

    public AdminAuthController(AdminAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Response<Map<String, String>> login(@RequestBody Map<String, String> body) {
        System.out.printf("Login Request: %s\n", body);
        Map<String, String> result = authService.login(body.get("username"), body.get("password"));
        return Response.ok(result);
    }
}
