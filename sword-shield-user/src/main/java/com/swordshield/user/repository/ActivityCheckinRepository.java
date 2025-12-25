package com.swordshield.user.repository;

import com.swordshield.user.domain.ActivityCheckin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivityCheckinRepository extends JpaRepository<ActivityCheckin, Long> {
    Optional<ActivityCheckin> findByActivityIdAndUserId(Long activityId, Long userId);
}

