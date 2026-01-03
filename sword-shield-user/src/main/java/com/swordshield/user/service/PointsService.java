package com.swordshield.user.service;

import com.swordshield.common.PageResult;
import com.swordshield.user.domain.ActivityCheckin;
import com.swordshield.user.domain.ActivityRef;
import com.swordshield.user.domain.PointsTxn;
import com.swordshield.user.domain.UserPoints;
import com.swordshield.user.dto.PointsSummaryDto;
import com.swordshield.user.dto.PointsTxnDto;
import com.swordshield.user.repository.ActivityCheckinRepository;
import com.swordshield.user.repository.ActivityRefRepository;
import com.swordshield.user.repository.PointsTxnRepository;
import com.swordshield.user.repository.UserPointsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 积分应用服务。
 *
 * <p>提供积分汇总、积分流水查询，以及活动签到得分等用例；相关领域对象包括：
 * {@link com.swordshield.user.domain.UserPoints}、{@link com.swordshield.user.domain.PointsTxn}、
 * {@link com.swordshield.user.domain.ActivityCheckin}。
 */
@Service
public class PointsService {
    private static final int ACTIVITY_CHECKIN_POINTS = 10;

    private final UserPointsRepository userPointsRepo;
    private final PointsTxnRepository pointsTxnRepo;
    private final ActivityRefRepository activityRefRepo;
    private final ActivityCheckinRepository activityCheckinRepo;

    public PointsService(UserPointsRepository userPointsRepo,
                         PointsTxnRepository pointsTxnRepo,
                         ActivityRefRepository activityRefRepo,
                         ActivityCheckinRepository activityCheckinRepo) {
        this.userPointsRepo = userPointsRepo;
        this.pointsTxnRepo = pointsTxnRepo;
        this.activityRefRepo = activityRefRepo;
        this.activityCheckinRepo = activityCheckinRepo;
    }

    public PointsSummaryDto getPoints(Long userId) {
        UserPoints userPoints = getOrCreateUserPoints(userId);
        PointsSummaryDto dto = new PointsSummaryDto();
        dto.setTotal(userPoints.getTotal());
        dto.setAvailable(userPoints.getAvailable());
        dto.setUsed(userPoints.getUsed());
        return dto;
    }

    public PageResult<PointsTxnDto> getPointsTxns(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size));
        Page<PointsTxn> p = pointsTxnRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<PointsTxnDto> items = p.getContent().stream().map(this::toTxnDto).toList();
        return PageResult.of(items, p.getTotalElements(), p.getNumber(), p.getSize());
    }

    public Map<String, Object> activityCheckinStatus(Long userId, Long activityId) {
        boolean checkedIn = activityCheckinRepo.findByActivityIdAndUserId(activityId, userId).isPresent();
        return Map.of("activityId", activityId, "checkedIn", checkedIn);
    }

    public Map<String, Object> activityCheckin(Long userId, Long activityId) {
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

        return Map.of(
                "earned", ACTIVITY_CHECKIN_POINTS,
                "total", userPoints.getTotal(),
                "available", userPoints.getAvailable(),
                "used", userPoints.getUsed()
        );
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

    private PointsTxnDto toTxnDto(PointsTxn txn) {
        PointsTxnDto dto = new PointsTxnDto();
        dto.setId(txn.getId());
        dto.setType(txn.getType());
        dto.setPoints(txn.getPoints());
        dto.setNote(txn.getNote());
        dto.setCreatedAt(txn.getCreatedAt());
        return dto;
    }
}
