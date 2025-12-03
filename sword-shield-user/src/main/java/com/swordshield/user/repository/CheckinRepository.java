package com.swordshield.user.repository;

import com.swordshield.user.domain.Checkin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface CheckinRepository extends JpaRepository<Checkin, Long> {
    Optional<Checkin> findByUserIdAndDate(Long userId, LocalDate date);
}

