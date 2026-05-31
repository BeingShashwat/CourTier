package com.courtier.courtier.case_.repository;

import com.courtier.courtier.case_.entity.UserCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserCaseRepository extends JpaRepository<UserCase, Long> {
    boolean existsByUserIdAndCourtCaseId(Long userId, Long caseId);
    Optional<UserCase> findByUserIdAndCourtCaseId(Long userId, Long caseId);
    long countByCourtCaseIdAndActiveTrue(Long caseId);

    @Query("SELECT uc FROM UserCase uc JOIN FETCH uc.user u WHERE uc.courtCase.cnrNumber = :cnrNumber AND uc.active = true")
    List<UserCase> findByCourtCaseCnrNumberAndActiveTrue(@Param("cnrNumber") String cnrNumber);

    void deleteAllByUserId(Long userId);
}