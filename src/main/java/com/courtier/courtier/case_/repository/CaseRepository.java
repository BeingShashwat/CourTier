package com.courtier.courtier.case_.repository;

import com.courtier.courtier.case_.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CaseRepository extends JpaRepository<Case, Long> {

    Optional<Case> findByCnrNumber(String cnrNumber);

    boolean existsByCnrNumber(String cnrNumber);

    // all cases a specific user is tracking
    @Query("SELECT uc.courtCase FROM UserCase uc WHERE uc.user.id = :userId AND uc.active = true")
    List<Case> findAllByUserId(@Param("userId") Long userId);

    // how many users are tracking a given case
    @Query("SELECT COUNT(uc) FROM UserCase uc WHERE uc.courtCase.id = :caseId AND uc.active = true")
    long countActiveTrackersByCaseId(@Param("caseId") Long caseId);

    // cases due for polling (not polled in last 6 hours)
    @Query("SELECT c FROM Case c WHERE c.lastPolledAt IS NULL OR c.lastPolledAt < :threshold")
    List<Case> findCasesDueForPolling(@Param("threshold") LocalDateTime threshold);
}