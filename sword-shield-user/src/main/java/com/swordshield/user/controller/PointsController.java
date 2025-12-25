package com.swordshield.user.controller;

import com.swordshield.common.ErrorCode;
import com.swordshield.common.JwtUtil;
import com.swordshield.common.Response;
import com.swordshield.user.domain.ActivityCheckin;
import com.swordshield.user.domain.ActivityRef;
import com.swordshield.user.domain.PointsTxn;
import com.swordshield.user.domain.UserPoints;
import com.swordshield.user.repository.ActivityCheckinRepository;
import com.swordshield.user.repository.ActivityRefRepository;
import com.swordshield.user.repository.PointsTxnRepository;
import com.swordshield.user.repository.UserPointsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/user")
@Tag(name = "积分", description = "积分查询与明细")
public class PointsController {
    private static final int ACTIVITY_CHECKIN_POINTS = 10;

    private final UserPointsRepository userPointsRepo;
    private final PointsTxnRepository pointsTxnRepo;
    private final ActivityRefRepository activityRefRepo;
    private final ActivityCheckinRepository activityCheckinRepo;

    public PointsController(
            UserPointsRepository userPointsRepo,
            PointsTxnRepository pointsTxnRepo,
            ActivityRefRepository activityRefRepo,
            ActivityCheckinRepository activityCheckinRepo
    ) {
        this.userPointsRepo = userPointsRepo;
        this.pointsTxnRepo = pointsTxnRepo;
        this.activityRefRepo = activityRefRepo;
        this.activityCheckinRepo = activityCheckinRepo;
    }

    @PostMapping("/checkin")
    @Operation(summary = "每日签到（已下线）", description = "每日签到功能已下线，请在活动详情页进行活动签到")
    public Response<Object> checkin(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
            @RequestHeader("Authorization") String authorization) {
        // Keep endpoint for backward compatibility but disable it on purpose.
        // Still parse token to ensure this is an authenticated request.
        JwtUtil.parseSubject(authorization.substring(7));
        return Response.error(ErrorCode.BAD_REQUEST.code, "每日签到已下线，请在活动详情页签到");
    }

    @GetMapping("/points")
    @Operation(summary = "查询积分", description = "返回用户累计积分，需携带 JWT")
    public Response<Map<String, Integer>> points(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
            @RequestHeader("Authorization") String authorization) {
        Long userId = Long.valueOf(JwtUtil.parseSubject(authorization.substring(7)));
        
        UserPoints userPoints = getOrCreateUserPoints(userId);
        
        return Response.ok(Map.of(
                "total", userPoints.getTotal(),
                "available", userPoints.getAvailable(),
                "used", userPoints.getUsed()
        ));
    }

    @GetMapping("/points/txns")
    @Operation(summary = "积分明细", description = "分页返回积分变更明细，需携带 JWT")
    public Response<Map<String, Object>> pointsTxns(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Long userId = Long.valueOf(JwtUtil.parseSubject(authorization.substring(7)));
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size));
        Page<PointsTxn> p = pointsTxnRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return Response.ok(Map.of(
                "items", p.getContent(),
                "total", p.getTotalElements(),
                "page", p.getNumber(),
                "size", p.getSize()
        ));
    }

    @GetMapping("/activity-checkin/status")
    @Operation(summary = "活动签到状态", description = "查询当前用户是否已对指定活动签到，需携带 JWT")
    public Response<Map<String, Object>> activityCheckinStatus(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @RequestParam("activityId") Long activityId) {
        Long userId = Long.valueOf(JwtUtil.parseSubject(authorization.substring(7)));
        boolean checkedIn = activityCheckinRepo.findByActivityIdAndUserId(activityId, userId).isPresent();
        return Response.ok(Map.of("activityId", activityId, "checkedIn", checkedIn));
    }

    @PostMapping("/activity-checkin")
    @Operation(summary = "活动签到", description = "仅活动期间可签到，成功后增加积分，需携带 JWT")
    public Response<Map<String, Object>> activityCheckin(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {token}", required = true)
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(JwtUtil.parseSubject(authorization.substring(7)));
        Object activityIdObj = body.get("activityId");
        if (activityIdObj == null) {
            throw new IllegalArgumentException("activityId 不能为空");
        }
        Long activityId = ((Number) activityIdObj).longValue();

        ActivityRef activity = activityRefRepo.findById(activityId).orElseThrow(() -> new IllegalArgumentException("活动不存在"));
        Instant now = Instant.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            throw new IllegalArgumentException("活动未开始，无法签到");
        }
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            throw new IllegalArgumentException("活动已结束，无法签到");
        }

        activityCheckinRepo.findByActivityIdAndUserId(activityId, userId).ifPresent(r -> {
            throw new IllegalArgumentException("已签到，无需重复操作");
        });

        ActivityCheckin checkin = new ActivityCheckin();
        checkin.setUserId(userId);
        checkin.setActivityId(activityId);
        checkin.setCheckedInAt(now);
        activityCheckinRepo.save(checkin);

        UserPoints userPoints = getOrCreateUserPoints(userId);
        userPoints.setTotal(userPoints.getTotal() + ACTIVITY_CHECKIN_POINTS);
        userPoints.setAvailable(userPoints.getAvailable() + ACTIVITY_CHECKIN_POINTS);
        userPoints.setUpdatedAt(now);
        userPointsRepo.save(userPoints);

        PointsTxn txn = new PointsTxn();
        txn.setUserId(userId);
        txn.setType("activity_checkin");
        txn.setPoints(ACTIVITY_CHECKIN_POINTS);
        txn.setNote("活动签到：" + (activity.getTitle() == null ? String.valueOf(activityId) : activity.getTitle()));
        txn.setCreatedAt(now);
        pointsTxnRepo.save(txn);

        return Response.ok(Map.of(
                "earned", ACTIVITY_CHECKIN_POINTS,
                "total", userPoints.getTotal(),
                "available", userPoints.getAvailable(),
                "used", userPoints.getUsed()
        ));
    }

    private UserPoints getOrCreateUserPoints(Long userId) {
        return userPointsRepo.findByUserId(userId)
                .orElseGet(() -> {
                    UserPoints newPoints = new UserPoints();
                    Instant now = Instant.now();
                    newPoints.setUserId(userId);
                    newPoints.setTotal(0);
                    newPoints.setAvailable(0);
                    newPoints.setUsed(0);
                    newPoints.setCreatedAt(now);
                    newPoints.setUpdatedAt(now);
                    userPointsRepo.save(newPoints);
                    return newPoints;
                });
    }
}

