package com.swordshield.user.controller;

import com.swordshield.common.AuthUtils;
import com.swordshield.common.ErrorCode;
import com.swordshield.common.PageResult;
import com.swordshield.common.Response;
import com.swordshield.user.dto.PointsSummaryDto;
import com.swordshield.user.dto.PointsTxnDto;
import com.swordshield.user.service.PointsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/user")
@Tag(name = "积分", description = "积分查询与明细")
public class PointsController {
    private final PointsService pointsService;

    public PointsController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @PostMapping("/checkin")
    @Operation(summary = "每日签到（已下线）", description = "每日签到功能已下线，请在活动详情页进行活动签到")
    public Response<Object> checkin(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
            @RequestHeader("Authorization") String authorization) {
        // Keep endpoint for backward compatibility but disable it on purpose.
        // Still parse token to ensure this is an authenticated request.
        AuthUtils.requireSubject(authorization);
        return Response.error(ErrorCode.BAD_REQUEST.code, "每日签到已下线，请在活动详情页签到");
    }

    @GetMapping("/points")
    @Operation(summary = "查询积分", description = "返回用户累计积分，需携带 JWT")
    public Response<PointsSummaryDto> points(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
            @RequestHeader("Authorization") String authorization) {
        Long userId = AuthUtils.requireUserId(authorization);
        return Response.ok(pointsService.getPoints(userId));
    }

    @GetMapping("/points/txns")
    @Operation(summary = "积分明细", description = "分页返回积分变更明细，需携带 JWT")
    public Response<PageResult<PointsTxnDto>> pointsTxns(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Long userId = AuthUtils.requireUserId(authorization);
        return Response.ok(pointsService.getPointsTxns(userId, page, size));
    }

    @GetMapping("/activity-checkin/status")
    @Operation(summary = "活动签到状态", description = "查询当前用户是否已对指定活动签到，需携带 JWT")
    public Response<Map<String, Object>> activityCheckinStatus(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @RequestParam("activityId") Long activityId) {
        Long userId = AuthUtils.requireUserId(authorization);
        return Response.ok(pointsService.activityCheckinStatus(userId, activityId));
    }

    @PostMapping("/activity-checkin")
    @Operation(summary = "活动签到", description = "仅活动期间可签到，成功后增加积分，需携带 JWT")
    public Response<Map<String, Object>> activityCheckin(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> body) {
        Long userId = AuthUtils.requireUserId(authorization);
        Object activityIdObj = body.get("activityId");
        if (activityIdObj == null) {
            throw new IllegalArgumentException("activityId 不能为空");
        }
        Long activityId = ((Number) activityIdObj).longValue();
        return Response.ok(pointsService.activityCheckin(userId, activityId));
    }
}
