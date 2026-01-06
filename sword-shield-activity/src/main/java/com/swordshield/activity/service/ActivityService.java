package com.swordshield.activity.service;

import com.swordshield.activity.domain.Activity;
import com.swordshield.activity.domain.ActivityRegistration;
import com.swordshield.activity.repository.ActivityRegistrationRepository;
import com.swordshield.activity.repository.ActivityRepository;
import com.swordshield.activity.repository.LikeRecordRepository;
import com.swordshield.api.activity.ActivityDto;
import com.swordshield.common.PageResult;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * 活动服务（用户侧用例）。
 *
 * <p>负责活动列表/详情查询、报名等用例，并基于领域对象
 * {@link com.swordshield.activity.domain.Activity} 与
 * {@link com.swordshield.activity.domain.ActivityRegistration} 完成数据读写。
 *
 * <p>对外返回的活动数据使用跨服务 DTO：{@link com.swordshield.api.activity.ActivityDto}。
 */
@Service
public class ActivityService {
    private final ActivityRepository activityRepo;
    private final ActivityRegistrationRepository regRepo;
    private final LikeRecordRepository likeRecordRepo;
    private final CacheManager cacheManager;

    public ActivityService(ActivityRepository activityRepo, ActivityRegistrationRepository regRepo, LikeRecordRepository likeRecordRepo, CacheManager cacheManager) {
        this.activityRepo = activityRepo;
        this.regRepo = regRepo;
        this.likeRecordRepo = likeRecordRepo;
        this.cacheManager = cacheManager;
    }

    public Page<Activity> list(int page, int size, String orderBy) {
        Sort sort = switch (orderBy == null ? "time" : orderBy) {
            case "heat" -> Sort.by(Sort.Direction.DESC, "heat");
            default -> Sort.by(Sort.Direction.ASC, "startTime");
        };
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size), sort);
        // Only return published activities (status = 1)
        return activityRepo.findByStatus(1, pageable);
    }

    public PageResult<ActivityDto> listPublicActivities(int page, int size, String orderBy) {
        String normalized = orderBy == null ? "time" : orderBy.trim();
        if ("heat".equalsIgnoreCase(normalized)) {
            // 热度为动态指标（报名+浏览+点赞），需要按动态热度全量排序后分页，保证排序正确。
            java.util.List<Activity> all = activityRepo.findByStatus(1, Sort.by(Sort.Direction.ASC, "id"));
            return computeAndPageByHeat(all, page, size);
        }

        Page<Activity> p = list(page, size, normalized);
        java.util.List<ActivityDto> dtos = toDtosWithDynamicHeat(p.getContent());
        return PageResult.of(dtos, p.getTotalElements(), p.getNumber(), p.getSize());
    }

    @Transactional
    public ActivityDto detail(Long id) {
        // 记录一次浏览（不依赖缓存，保证浏览量与热度动态更新）。
        activityRepo.incrementViews(id);

        Activity a = activityRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("活动不存在"));
        return toDtoWithDynamicHeat(a);
    }

    public boolean isRegistered(Long userId, Long activityId) {
        return regRepo.findByActivityIdAndUserId(activityId, userId).isPresent();
    }

    @Transactional
    public void register(Long userId, Long activityId, String channel) {
        Activity act = activityRepo.findById(activityId).orElseThrow(() ->
            new IllegalArgumentException("活动不存在"));
        Instant now = Instant.now();
        if (act.getEndTime() != null && now.isAfter(act.getEndTime())) {
            throw new IllegalArgumentException("活动已结束，无法报名");
        }
        if (act.getStartTime() != null && !now.isBefore(act.getStartTime())) {
            throw new IllegalArgumentException("活动已开始，报名已截止");
        }
        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityId(activityId);
        reg.setUserId(userId);
        reg.setChannel(channel == null ? "web" : channel);
        reg.setRegisteredAt(Instant.now());
        try {
            regRepo.save(reg);
            regRepo.flush();
        } catch (DataIntegrityViolationException e) {
            if (regRepo.findByActivityIdAndUserId(activityId, userId).isPresent()) {
                throw new IllegalArgumentException("您已报名该活动，无需重复报名");
            }
            throw e;
        }

        int updated = activityRepo.incrementCurrentRegistrationsIfAvailable(activityId);
        if (updated == 0) {
            throw new IllegalArgumentException("活动报名人数已满");
        }
    }

    public Map<String, Object> getStatistics(Long activityId) {
        Activity activity = activityRepo.findById(activityId).orElseThrow(() -> new IllegalArgumentException("活动不存在"));
        long registrationCount = regRepo.countByActivityId(activityId);

        long likeCount = likeRecordRepo.countLikesByTargetIds("activity", java.util.List.of(activityId)).stream()
            .findFirst()
            .map(LikeRecordRepository.LikeCount::getCnt)
            .orElse(0L);
        long views = activity.getViews() == null ? 0L : activity.getViews();
        long dynamicHeat = registrationCount + views + likeCount;

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("activityId", activityId);
        stats.put("activityTitle", activity.getTitle());
        stats.put("registrationCount", registrationCount);
        stats.put("maxRegistrations", activity.getMaxRegistrations());
        stats.put("currentRegistrations", activity.getCurrentRegistrations());
        stats.put("heat", dynamicHeat);
        stats.put("startTime", activity.getStartTime());
        stats.put("endTime", activity.getEndTime());
        stats.put("location", activity.getLocation());

        // Calculate registration rate
        if (activity.getMaxRegistrations() != null && activity.getMaxRegistrations() > 0) {
            double rate = (double) registrationCount / activity.getMaxRegistrations() * 100;
            stats.put("registrationRate", Math.round(rate * 100.0) / 100.0);
        } else {
            stats.put("registrationRate", 0);
        }

        return stats;
    }

    public java.util.List<Map<String, Object>> getAllStatistics() {
        java.util.List<Activity> activities = activityRepo.findAll(Sort.by(Sort.Direction.DESC, "heat"));

        java.util.Map<Long, Long> registrationCountByActivityId = java.util.Collections.emptyMap();
        java.util.List<Long> activityIds = activities.stream().map(Activity::getId).toList();
        if (!activityIds.isEmpty()) {
            registrationCountByActivityId = regRepo.countRegistrationsByActivityIds(activityIds).stream()
                    .collect(java.util.stream.Collectors.toMap(
                            ActivityRegistrationRepository.ActivityRegistrationCount::getActivityId,
                            ActivityRegistrationRepository.ActivityRegistrationCount::getRegistrations
                    ));
        }

        java.util.List<Map<String, Object>> statsList = new java.util.ArrayList<>();

        java.util.Map<Long, Long> likeCountByActivityId = java.util.Collections.emptyMap();
        if (!activityIds.isEmpty()) {
            likeCountByActivityId = likeRecordRepo.countLikesByTargetIds("activity", activityIds).stream()
                    .collect(java.util.stream.Collectors.toMap(
                            LikeRecordRepository.LikeCount::getTargetId,
                            LikeRecordRepository.LikeCount::getCnt
                    ));
        }

        for (Activity activity : activities) {
            long registrationCount = registrationCountByActivityId.getOrDefault(activity.getId(), 0L);
            long likeCount = likeCountByActivityId.getOrDefault(activity.getId(), 0L);
            long views = activity.getViews() == null ? 0L : activity.getViews();
            long dynamicHeat = registrationCount + views + likeCount;
            
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("activityId", activity.getId());
            stats.put("activityTitle", activity.getTitle());
            stats.put("registrationCount", registrationCount);
            stats.put("maxRegistrations", activity.getMaxRegistrations());
            stats.put("heat", dynamicHeat);
            stats.put("startTime", activity.getStartTime());

            if (activity.getMaxRegistrations() != null && activity.getMaxRegistrations() > 0) {
                double rate = (double) registrationCount / activity.getMaxRegistrations() * 100;
                stats.put("registrationRate", Math.round(rate * 100.0) / 100.0);
            } else {
                stats.put("registrationRate", 0);
            }

            statsList.add(stats);
        }

        return statsList;
    }

    public Page<Map<String, Object>> getMyRegisteredActivities(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size), Sort.by(Sort.Direction.DESC, "registeredAt"));
        Page<ActivityRegistration> registrations = regRepo.findByUserId(userId, pageable);

        // Get activity IDs
        java.util.List<Long> activityIds = registrations.getContent().stream()
            .map(ActivityRegistration::getActivityId)
            .toList();

        // Fetch activities
        java.util.List<Activity> activities = activityRepo.findAllById(activityIds);

        // Create a map for quick lookup
        java.util.Map<Long, Activity> activityMap = activities.stream()
            .collect(java.util.stream.Collectors.toMap(Activity::getId, a -> a));

        // Create registration map for channel lookup
        java.util.Map<Long, ActivityRegistration> registrationMap = registrations.getContent().stream()
            .collect(java.util.stream.Collectors.toMap(ActivityRegistration::getActivityId, reg -> reg));

        // Combine activity data with registration channel
        java.util.List<Map<String, Object>> result = registrations.getContent().stream()
            .map(reg -> {
                Activity activity = activityMap.get(reg.getActivityId());
                if (activity == null) return null;

                Map<String, Object> item = new java.util.HashMap<>();
                item.put("id", activity.getId());
                item.put("title", activity.getTitle());
                item.put("summary", activity.getSummary());
                item.put("coverUrl", activity.getCoverUrl());
                item.put("startTime", activity.getStartTime());
                item.put("endTime", activity.getEndTime());
                item.put("location", activity.getLocation());
                item.put("speaker", activity.getSpeaker());
                item.put("liveUrl", activity.getLiveUrl());
                item.put("replayUrl", activity.getReplayUrl());
                item.put("maxRegistrations", activity.getMaxRegistrations());
                item.put("currentRegistrations", activity.getCurrentRegistrations());
                item.put("heat", activity.getHeat());
                item.put("status", activity.getStatus());
                item.put("registrationChannel", reg.getChannel());
                item.put("registeredAt", reg.getRegisteredAt());

                return item;
            })
            .filter(item -> item != null)
            .toList();

        return new org.springframework.data.domain.PageImpl<>(
            result,
            pageable,
            registrations.getTotalElements()
        );
    }

    public java.util.List<ActivityDto> listDtos(int page, int size, String orderBy) {
        Page<Activity> p = list(page, size, orderBy);
        return p.getContent().stream().map(this::toDto).toList();
    }

    public org.springframework.data.domain.Page<ActivityDto> listDtoPage(int page, int size, String orderBy) {
        Page<Activity> p = list(page, size, orderBy);
        return p.map(this::toDto);
    }

    private ActivityDto toDto(Activity a) {
        ActivityDto dto = new ActivityDto();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setSummary(a.getSummary());
        dto.setCoverUrl(a.getCoverUrl());
        dto.setStartTime(a.getStartTime());
        dto.setEndTime(a.getEndTime());
        dto.setLocation(a.getLocation());
        dto.setSpeaker(a.getSpeaker());
        dto.setLiveUrl(a.getLiveUrl());
        dto.setReplayUrl(a.getReplayUrl());
        dto.setMaxRegistrations(a.getMaxRegistrations());
        dto.setCurrentRegistrations(a.getCurrentRegistrations());
        dto.setHeat(a.getHeat());
        dto.setStatus(a.getStatus());
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }

    private java.util.List<ActivityDto> toDtosWithDynamicHeat(java.util.List<Activity> activities) {
        if (activities == null || activities.isEmpty()) {
            return java.util.List.of();
        }

        java.util.List<Long> ids = activities.stream().map(Activity::getId).toList();

        java.util.Map<Long, Long> registrationsById = regRepo.countRegistrationsByActivityIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(
                        ActivityRegistrationRepository.ActivityRegistrationCount::getActivityId,
                        ActivityRegistrationRepository.ActivityRegistrationCount::getRegistrations
                ));

        java.util.Map<Long, Long> likesById = likeRecordRepo.countLikesByTargetIds("activity", ids).stream()
                .collect(java.util.stream.Collectors.toMap(
                        LikeRecordRepository.LikeCount::getTargetId,
                        LikeRecordRepository.LikeCount::getCnt
                ));

        return activities.stream().map((a) -> {
            long registrations = registrationsById.getOrDefault(a.getId(), a.getCurrentRegistrations() == null ? 0L : a.getCurrentRegistrations().longValue());
            long views = a.getViews() == null ? 0L : a.getViews();
            long likes = likesById.getOrDefault(a.getId(), 0L);
            long heat = registrations + views + likes;
            ActivityDto dto = toDto(a);
            dto.setHeat((int) Math.min(Integer.MAX_VALUE, heat));
            return dto;
        }).toList();
    }

    private ActivityDto toDtoWithDynamicHeat(Activity a) {
        java.util.List<ActivityDto> list = toDtosWithDynamicHeat(java.util.List.of(a));
        return list.isEmpty() ? toDto(a) : list.get(0);
    }

    private PageResult<ActivityDto> computeAndPageByHeat(java.util.List<Activity> activities, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));

        java.util.List<ActivityDto> dtos = toDtosWithDynamicHeat(activities);
        dtos = dtos.stream()
                .sorted((a, b) -> {
                    int ha = a.getHeat() == null ? 0 : a.getHeat();
                    int hb = b.getHeat() == null ? 0 : b.getHeat();
                    int cmp = Integer.compare(hb, ha);
                    if (cmp != 0) return cmp;
                    // 热度相同按 id 倒序，稳定一些
                    long ia = a.getId() == null ? 0 : a.getId();
                    long ib = b.getId() == null ? 0 : b.getId();
                    return Long.compare(ib, ia);
                })
                .toList();

        long total = dtos.size();
        int from = Math.min(dtos.size(), safePage * safeSize);
        int to = Math.min(dtos.size(), from + safeSize);
        java.util.List<ActivityDto> pageItems = dtos.subList(from, to);
        return PageResult.of(pageItems, total, safePage, safeSize);
    }
}
