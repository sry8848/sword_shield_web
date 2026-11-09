package com.swordshield.user.controller;

import com.swordshield.common.ErrorCode;
import com.swordshield.common.JwtUtil;
import com.swordshield.common.Response;
import com.swordshield.user.domain.Checkin;
import com.swordshield.user.domain.UserPoints;
import com.swordshield.user.repository.CheckinRepository;
import com.swordshield.user.repository.UserPointsRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class PointsController {
    private final UserPointsRepository userPointsRepo;
    private final CheckinRepository checkinRepo;

    public PointsController(UserPointsRepository userPointsRepo, CheckinRepository checkinRepo) {
        this.userPointsRepo = userPointsRepo;
        this.checkinRepo = checkinRepo;
    }

    @PostMapping("/checkin")
    public Response<Object> checkin(@RequestHeader("Authorization") String authorization) {
        Long userId = Long.valueOf(JwtUtil.parseSubject(authorization.substring(7)));
        LocalDate today = LocalDate.now();
        
        // Check if already checked in today
        if (checkinRepo.findByUserIdAndDate(userId, today).isPresent()) {
            return Response.error(ErrorCode.BAD_REQUEST.code, "今日已签到");
        }
        
        // Save check-in record
        Checkin c = new Checkin();
        c.setUserId(userId);
        c.setDate(today);
        checkinRepo.save(c);
        
        // Update user points
        UserPoints userPoints = userPointsRepo.findByUserId(userId)
            .orElseGet(() -> {
                UserPoints newPoints = new UserPoints();
                newPoints.setUserId(userId);
                newPoints.setTotal(0);
                newPoints.setAvailable(0);
                newPoints.setUsed(0);
                newPoints.setCreatedAt(Instant.now());
                return newPoints;
            });
        
        userPoints.setTotal(userPoints.getTotal() + 10);
        userPoints.setAvailable(userPoints.getAvailable() + 10);
        userPoints.setUpdatedAt(Instant.now());
        userPointsRepo.save(userPoints);
        
        return Response.ok(null);
    }

    @GetMapping("/points")
    public Response<Map<String, Integer>> points(@RequestHeader("Authorization") String authorization) {
        Long userId = Long.valueOf(JwtUtil.parseSubject(authorization.substring(7)));
        
        UserPoints userPoints = userPointsRepo.findByUserId(userId)
            .orElseGet(() -> {
                UserPoints newPoints = new UserPoints();
                newPoints.setUserId(userId);
                newPoints.setTotal(0);
                newPoints.setAvailable(0);
                newPoints.setUsed(0);
                newPoints.setCreatedAt(Instant.now());
                newPoints.setUpdatedAt(Instant.now());
                userPointsRepo.save(newPoints);
                return newPoints;
            });
        
        return Response.ok(Map.of("total", userPoints.getTotal()));
    }
}

