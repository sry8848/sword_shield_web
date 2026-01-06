package com.swordshield.activity.service;

import com.swordshield.activity.domain.Activity;
import com.swordshield.activity.repository.ActivityRepository;
import com.swordshield.api.activity.ActivityDto;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 活动后台管理服务（管理员侧用例）。
 *
 * <p>提供活动的增删改查，并负责相关缓存失效；对外数据结构沿用
 * {@link com.swordshield.api.activity.ActivityDto} 作为管理端与活动服务之间的契约 DTO。
 */
@Service
public class ActivityAdminService {
    private final ActivityRepository activityRepo;

    public ActivityAdminService(ActivityRepository activityRepo) {
        this.activityRepo = activityRepo;
    }

    public java.util.List<ActivityDto> getAll() {
        return activityRepo.findAll(Sort.by(Sort.Direction.DESC, "id")).stream().map(this::toDto).toList();
    }

    public Page<ActivityDto> list(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size),
                Sort.by(Sort.Direction.DESC, "id"));
        return activityRepo.findAll(pageable).map(this::toDto);
    }

    public ActivityDto getById(Long id) {
        return toDto(activityRepo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("活动不存在")));
    }

    @CacheEvict(cacheNames = "activity:list", allEntries = true)
    public ActivityDto create(ActivityDto activity) {
        Activity entity = new Activity();
        entity.setTitle(activity.getTitle());
        entity.setSummary(activity.getSummary());
        entity.setCoverUrl(activity.getCoverUrl());
        entity.setStartTime(activity.getStartTime());
        entity.setEndTime(activity.getEndTime());
        entity.setLocation(activity.getLocation());
        entity.setSpeaker(activity.getSpeaker());
        entity.setLiveUrl(activity.getLiveUrl());
        entity.setReplayUrl(activity.getReplayUrl());
        entity.setMaxRegistrations(activity.getMaxRegistrations());
        entity.setCurrentRegistrations(activity.getCurrentRegistrations());
        entity.setHeat(activity.getHeat());
        entity.setStatus(activity.getStatus());

        if (entity.getTitle() == null || entity.getTitle().isBlank()) {
            throw new IllegalArgumentException("活动标题不能为空");
        }

        entity.setId(null);
        if (entity.getCurrentRegistrations() == null) {
            entity.setCurrentRegistrations(0);
        }
        if (entity.getViews() == null) {
            entity.setViews(0);
        }
        if (entity.getHeat() == null) {
            entity.setHeat(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        return toDto(activityRepo.save(entity));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "activity:list", allEntries = true),
            @CacheEvict(cacheNames = "activity:detail", key = "'id=' + #id")
    })
    public ActivityDto update(Long id, ActivityDto activity) {
        Activity existing = activityRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("活动不存在"));

        if (activity.getTitle() != null && !activity.getTitle().isBlank()) {
            existing.setTitle(activity.getTitle());
        }
        if (activity.getSummary() != null) {
            existing.setSummary(activity.getSummary());
        }
        if (activity.getCoverUrl() != null) {
            existing.setCoverUrl(activity.getCoverUrl());
        }
        if (activity.getStartTime() != null) {
            existing.setStartTime(activity.getStartTime());
        }
        if (activity.getEndTime() != null) {
            existing.setEndTime(activity.getEndTime());
        }
        if (activity.getLocation() != null) {
            existing.setLocation(activity.getLocation());
        }
        if (activity.getSpeaker() != null) {
            existing.setSpeaker(activity.getSpeaker());
        }
        if (activity.getLiveUrl() != null) {
            existing.setLiveUrl(activity.getLiveUrl());
        }
        if (activity.getReplayUrl() != null) {
            existing.setReplayUrl(activity.getReplayUrl());
        }
        if (activity.getMaxRegistrations() != null) {
            existing.setMaxRegistrations(activity.getMaxRegistrations());
        }
        if (activity.getCurrentRegistrations() != null) {
            existing.setCurrentRegistrations(activity.getCurrentRegistrations());
        }
        if (activity.getHeat() != null) {
            existing.setHeat(activity.getHeat());
        }
        if (activity.getStatus() != null) {
            existing.setStatus(activity.getStatus());
        }

        existing.setUpdatedAt(Instant.now());
        return toDto(activityRepo.save(existing));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "activity:list", allEntries = true),
            @CacheEvict(cacheNames = "activity:detail", key = "'id=' + #id")
    })
    public void delete(Long id) {
        Activity activity = activityRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("活动不存在"));
        activityRepo.delete(activity);
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
}
