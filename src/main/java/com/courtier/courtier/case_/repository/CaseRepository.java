package com.courtier.courtier.case_.repository;

import com.courtier.courtier.case_.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CaseRepository extends JpaRepository<Case, Long> {

    Optional<Case> findByCnrNumber(String cnrNumber);

    boolean existsByCnrNumber(String cnrNumber);

    @Query("SELECT uc.courtCase FROM UserCase uc WHERE uc.user.id = :userId AND uc.active = true")
    List<Case> findAllByUserId(@Param("userId") Long userId);
}
