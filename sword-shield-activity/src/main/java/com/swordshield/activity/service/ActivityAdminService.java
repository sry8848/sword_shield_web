package com.swordshield.activity.service;

import com.swordshield.activity.domain.Activity;
import com.swordshield.activity.repository.ActivityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ActivityAdminService {
    private final ActivityRepository activityRepo;

    public ActivityAdminService(ActivityRepository activityRepo) {
        this.activityRepo = activityRepo;
    }

    public java.util.List<Activity> getAll() {
        return activityRepo.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public Page<Activity> list(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size),
                Sort.by(Sort.Direction.DESC, "id"));
        return activityRepo.findAll(pageable);
    }

    public Activity getById(Long id) {
        return activityRepo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("活动不存在"));
    }

    public Activity create(Activity activity) {
        if (activity.getTitle() == null || activity.getTitle().isBlank()) {
            throw new IllegalArgumentException("活动标题不能为空");
        }

        activity.setId(null);
        if (activity.getCurrentRegistrations() == null) {
            activity.setCurrentRegistrations(0);
        }
        if (activity.getHeat() == null) {
            activity.setHeat(0);
        }
        if (activity.getStatus() == null) {
            activity.setStatus(1);
        }
        Instant now = Instant.now();
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);

        return activityRepo.save(activity);
    }

    public Activity update(Long id, Activity activity) {
        Activity existing = getById(id);

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
        return activityRepo.save(existing);
    }

    public void delete(Long id) {
        Activity activity = getById(id);
        activityRepo.delete(activity);
    }
}

