package com.swordshield.activity.repository;

import com.swordshield.activity.domain.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByStatus(int status, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Activity a
               set a.currentRegistrations = coalesce(a.currentRegistrations, 0) + 1
             where a.id = :id
               and (a.maxRegistrations is null or coalesce(a.currentRegistrations, 0) < a.maxRegistrations)
            """)
    int incrementCurrentRegistrationsIfAvailable(@Param("id") Long id);
}

