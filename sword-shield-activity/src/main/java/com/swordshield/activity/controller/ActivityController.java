package com.swordshield.activity.controller;

import com.swordshield.activity.dto.ActivityDto;
import com.swordshield.activity.dto.ActivityRegisterRequest;
import com.swordshield.activity.service.ActivityService;
import com.swordshield.common.AuthUtils;
import com.swordshield.common.Response;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/activity")
@Tag(name = "活动", description = "活动查询、报名与统计")
public class ActivityController {
    private final ActivityService service;

    public ActivityController(ActivityService service) { this.service = service; }

    @GetMapping("/list")
    @Operation(summary = "分页查询活动列表", description = "支持分页和排序")
    public Response<Map<String, Object>> list(@RequestParam(name = "page", defaultValue = "0") int page,
                                              @RequestParam(name = "size", defaultValue = "10") int size,
                                              @RequestParam(name = "orderBy", required = false) String orderBy) {
        Page<ActivityDto> p = service.listDtoPage(page, size, orderBy);
        return Response.ok(Map.of(
                "items", p.getContent(),
                "total", p.getTotalElements(),
                "page", p.getNumber(),
                "size", p.getSize()
        ));
    }

    @GetMapping("/detail")
    @Operation(summary = "活动详情", description = "根据活动 ID 获取详情")
    public Response<ActivityDto> detail(@RequestParam("id") Long id) {
        return Response.ok(service.detail(id));
    }

    @PostMapping("/register")
    @Operation(summary = "报名活动", description = "需携带用户 JWT，支持指定渠道")
    public Response<Object> register(
                                     @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
                                     @RequestHeader(name = "Authorization", required = false) String authorization,
                                     @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
                                     @Valid @RequestBody ActivityRegisterRequest body) {
        Long activityId = body.getActivityId();
        String channel = body.getChannel() == null || body.getChannel().isBlank() ? "web" : body.getChannel().trim();
        Long userId = AuthUtils.requireUserId(userIdHeader, authorization);
        service.register(userId, activityId, channel);
        return Response.ok(null);
    }

    @GetMapping("/registration/status")
    @Operation(summary = "报名状态", description = "查询当前用户是否已报名指定活动，需携带用户 JWT")
    public Response<Map<String, Object>> registrationStatus(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestParam("activityId") Long activityId) {
        Long userId = AuthUtils.requireUserId(userIdHeader, authorization);
        boolean registered = service.isRegistered(userId, activityId);
        return Response.ok(Map.of("activityId", activityId, "registered", registered));
    }

    @GetMapping("/statistics")
    @Operation(summary = "活动统计数据", description = "返回单个活动的报名、点赞等统计信息")
    public Response<Map<String, Object>> statistics(@RequestParam("id") Long id) {
        Map<String, Object> stats = service.getStatistics(id);
        return Response.ok(stats);
    }

    @GetMapping("/statistics/all")
    @Operation(summary = "全部活动统计", description = "返回所有活动的基础统计列表")
    public Response<java.util.List<Map<String, Object>>> allStatistics() {
        java.util.List<Map<String, Object>> stats = service.getAllStatistics();
        return Response.ok(stats);
    }

    @GetMapping("/my-registrations")
    @Operation(summary = "我的报名记录", description = "需携带用户 JWT，分页返回本人报名的活动")
    public Response<Map<String, Object>> myRegistrations(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Long userId = AuthUtils.requireUserId(userIdHeader, authorization);
        Page<Map<String, Object>> p = service.getMyRegisteredActivities(userId, page, size);
        return Response.ok(Map.of(
                "items", p.getContent(),
                "total", p.getTotalElements(),
                "page", p.getNumber(),
                "size", p.getSize()
        ));
    }
}
