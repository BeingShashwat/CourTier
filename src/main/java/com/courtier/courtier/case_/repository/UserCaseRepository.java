package com.courtier.courtier.case_.repository;

import com.courtier.courtier.case_.entity.UserCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCaseRepository extends JpaRepository<UserCase, Long> {

    boolean existsByUserIdAndCourtCaseId(Long userId, Long caseId);

    Optional<UserCase> findByUserIdAndCourtCaseId(Long userId, Long caseId);
}
