package com.swordshield.activity.repository;

import com.swordshield.activity.domain.ActivityRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ActivityRegistrationRepository extends JpaRepository<ActivityRegistration, Long> {
    Optional<ActivityRegistration> findByActivityIdAndUserId(Long activityId, Long userId);
    long countByActivityId(Long activityId);
    List<ActivityRegistration> findByUserId(Long userId);
    Page<ActivityRegistration> findByUserId(Long userId, Pageable pageable);
}

