package com.swordshield.admin.controller;

import com.swordshield.activity.domain.Activity;
import com.swordshield.admin.service.AdminActivityService;
import com.swordshield.common.JwtUtil;
import com.swordshield.common.Response;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/activities")
public class AdminActivityController {
    private final AdminActivityService activityService;

    public AdminActivityController(AdminActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * 验证管理员权限
     */
    private void verifyAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new SecurityException("未登录，请先登录");
        }
        String token = authorization.substring(7);
        String subject = JwtUtil.parseSubject(token);
        if (!subject.startsWith("admin_")) {
            throw new SecurityException("无权限访问，需要管理员权限");
        }
    }

    @GetMapping
    public Response<java.util.List<Activity>> getAll(
            @RequestHeader("Authorization") String authorization) {
        verifyAdmin(authorization);
        return Response.ok(activityService.getAll());
    }

    @GetMapping("/list")
    public Response<Map<String, Object>> list(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        verifyAdmin(authorization);
        
        Page<Activity> p = activityService.list(page, size);
        return Response.ok(Map.of(
            "items", p.getContent(),
            "total", p.getTotalElements(),
            "page", p.getNumber(),
            "size", p.getSize()
        ));
    }

    @GetMapping("/{id}")
    public Response<Activity> get(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") Long id) {
        verifyAdmin(authorization);
        return Response.ok(activityService.getById(id));
    }

    @PostMapping
    public Response<Activity> create(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Activity activity) {
        verifyAdmin(authorization);
        Activity created = activityService.create(activity);
        return Response.ok(created);
    }

    @PutMapping("/{id}")
    public Response<Activity> update(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") Long id,
            @RequestBody Activity activity) {
        verifyAdmin(authorization);
        Activity updated = activityService.update(id, activity);
        return Response.ok(updated);
    }

    @DeleteMapping("/{id}")
    public Response<Object> delete(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") Long id) {
        verifyAdmin(authorization);
        activityService.delete(id);
        return Response.ok(null);
    }
}
