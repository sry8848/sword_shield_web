package com.swordshield.content.repository;

import com.swordshield.content.domain.PosterJob;
import com.swordshield.content.domain.PosterJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface PosterJobRepository extends JpaRepository<PosterJob, String> {
    @Modifying
    @Transactional
    @Query("UPDATE PosterJob j SET j.status = :next, j.updatedAt = :now WHERE j.id = :id AND j.status = :expected")
    int transitionStatus(@Param("id") String id,
                         @Param("expected") PosterJobStatus expected,
                         @Param("next") PosterJobStatus next,
                         @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("""
            UPDATE PosterJob j
            SET j.dispatchAttempts = j.dispatchAttempts + 1,
                j.dispatchLastAttemptAt = :now,
                j.dispatchLastError = 'DISPATCHING',
                j.dispatchNextRetryAt = :lockUntil,
                j.updatedAt = :now
            WHERE j.id = :id
              AND j.status = :expectedStatus
              AND (j.dispatchAttempts = 0 OR j.dispatchLastError IS NOT NULL)
              AND (j.dispatchNextRetryAt IS NULL OR j.dispatchNextRetryAt <= :now)
              AND j.dispatchAttempts < :maxAttempts
            """)
    int claimDispatchAttempt(@Param("id") String id,
                             @Param("expectedStatus") PosterJobStatus expectedStatus,
                             @Param("now") Instant now,
                             @Param("lockUntil") Instant lockUntil,
                             @Param("maxAttempts") int maxAttempts);

    @Query("""
            SELECT j FROM PosterJob j
            WHERE j.status = :expectedStatus
              AND (j.dispatchAttempts = 0 OR j.dispatchLastError IS NOT NULL)
              AND (j.dispatchNextRetryAt IS NULL OR j.dispatchNextRetryAt <= :now)
            ORDER BY j.createdAt ASC
            """)
    List<PosterJob> findDueDispatchJobs(@Param("expectedStatus") PosterJobStatus expectedStatus,
                                        @Param("now") Instant now,
                                        Pageable pageable);
}
