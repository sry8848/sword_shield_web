package com.swordshield.activity.controller;

import com.swordshield.activity.domain.Activity;
import com.swordshield.activity.service.ActivityService;
import com.swordshield.common.JwtUtil;
import com.swordshield.common.Response;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {
    private final ActivityService service;

    public ActivityController(ActivityService service) { this.service = service; }

    @GetMapping("/list")
    public Response<Map<String, Object>> list(@RequestParam(name = "page", defaultValue = "0") int page,
                                              @RequestParam(name = "size", defaultValue = "10") int size,
                                              @RequestParam(name = "orderBy", required = false) String orderBy) {
        Page<Activity> p = service.list(page, size, orderBy);
        return Response.ok(Map.of(
                "items", p.getContent(),
                "total", p.getTotalElements(),
                "page", p.getNumber(),
                "size", p.getSize()
        ));
    }

    @GetMapping("/detail")
    public Response<Activity> detail(@RequestParam("id") Long id) {
        return Response.ok(service.detail(id));
    }

    @PostMapping("/register")
    public Response<Object> register(@RequestHeader("Authorization") String authorization,
                                     @RequestBody Map<String, Object> body) {
        Long activityId = ((Number) body.get("activityId")).longValue();
        String channel = (String) body.getOrDefault("channel", "web");
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
        Long userId = Long.valueOf(JwtUtil.parseSubject(token));
        service.register(userId, activityId, channel);
        return Response.ok(null);
    }

    @GetMapping("/statistics")
    public Response<Map<String, Object>> statistics(@RequestParam("id") Long id) {
        Map<String, Object> stats = service.getStatistics(id);
        return Response.ok(stats);
    }

    @GetMapping("/statistics/all")
    public Response<java.util.List<Map<String, Object>>> allStatistics() {
        java.util.List<Map<String, Object>> stats = service.getAllStatistics();
        return Response.ok(stats);
    }

    @GetMapping("/my-registrations")
    public Response<Map<String, Object>> myRegistrations(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
        Long userId = Long.valueOf(JwtUtil.parseSubject(token));
        Page<Map<String, Object>> p = service.getMyRegisteredActivities(userId, page, size);
        return Response.ok(Map.of(
                "items", p.getContent(),
                "total", p.getTotalElements(),
                "page", p.getNumber(),
                "size", p.getSize()
        ));
    }
}
