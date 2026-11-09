package com.swordshield.user.repository;

import com.swordshield.user.domain.PointsTxn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointsTxnRepository extends JpaRepository<PointsTxn, Long> {
    @Query("select coalesce(sum(p.points),0) from PointsTxn p where p.userId = :userId")
    int sumPointsByUserId(@Param("userId") Long userId);
}

