package com.swordshield.activity.repository;

import com.swordshield.activity.domain.ActivityRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ActivityRegistrationRepository extends JpaRepository<ActivityRegistration, Long> {
    Optional<ActivityRegistration> findByActivityIdAndUserId(Long activityId, Long userId);
    long countByActivityId(Long activityId);

    @Query("select r.activityId as activityId, count(r) as registrations from ActivityRegistration r where r.activityId in :activityIds group by r.activityId")
    List<ActivityRegistrationCount> countRegistrationsByActivityIds(@Param("activityIds") List<Long> activityIds);

    interface ActivityRegistrationCount {
        Long getActivityId();
        Long getRegistrations();
    }

    List<ActivityRegistration> findByUserId(Long userId);
    Page<ActivityRegistration> findByUserId(Long userId, Pageable pageable);
}

