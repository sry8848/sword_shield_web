package com.swordshield.admin.controller;

import com.swordshield.activity.domain.Activity;
import com.swordshield.admin.service.AdminActivityService;
import com.swordshield.common.JwtUtil;
import com.swordshield.common.Response;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/activities")
@Tag(name = "活动管理（管理员）", description = "管理员查看、创建、修改、删除活动")
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
    @Operation(summary = "全部活动列表", description = "管理员查询全部活动")
    public Response<java.util.List<Activity>> getAll(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader("Authorization") String authorization) {
        verifyAdmin(authorization);
        return Response.ok(activityService.getAll());
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询活动", description = "管理员分页查询活动列表")
    public Response<Map<String, Object>> list(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
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
    @Operation(summary = "活动详情（管理员）", description = "根据 ID 查询活动详情")
    public Response<Activity> get(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") Long id) {
        verifyAdmin(authorization);
        return Response.ok(activityService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建活动", description = "管理员创建新活动")
    public Response<Activity> create(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @RequestBody Activity activity) {
        verifyAdmin(authorization);
        Activity created = activityService.create(activity);
        return Response.ok(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新活动", description = "管理员修改活动信息")
    public Response<Activity> update(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") Long id,
            @RequestBody Activity activity) {
        verifyAdmin(authorization);
        Activity updated = activityService.update(id, activity);
        return Response.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除活动", description = "管理员删除活动")
    public Response<Object> delete(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") Long id) {
        verifyAdmin(authorization);
        activityService.delete(id);
        return Response.ok(null);
    }
}
