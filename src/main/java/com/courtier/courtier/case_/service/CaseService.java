package com.courtier.courtier.case_.service;

import com.courtier.courtier.case_.dto.AddCaseRequest;
import com.courtier.courtier.case_.dto.CaseResponse;
import com.courtier.courtier.case_.entity.Case;
import com.courtier.courtier.case_.entity.UserCase;
import com.courtier.courtier.case_.repository.CaseRepository;
import com.courtier.courtier.case_.repository.UserCaseRepository;
import com.courtier.courtier.common.exception.CourtierException;
import com.courtier.courtier.user.entity.User;
import com.courtier.courtier.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {

    private static final int MAX_TRACKERS_PER_CASE = 5;

    private final CaseRepository caseRepository;
    private final UserCaseRepository userCaseRepository;
    private final UserRepository userRepository;

    @Transactional
    public CaseResponse addCase(String userEmail, AddCaseRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CourtierException.NotFound("User not found"));

        // if CNR already exists in DB, just link the user to it
        Case courtCase = caseRepository.findByCnrNumber(request.cnrNumber())
                .orElse(null);

        if (courtCase != null) {
            // check if user already tracking this case
            if (userCaseRepository.existsByUserIdAndCourtCaseId(user.getId(), courtCase.getId())) {
                throw new CourtierException.Conflict("You are already tracking this case");
            }
            // enforce 5-user cap
            long trackerCount = userCaseRepository.countByCourtCaseIdAndActiveTrue(courtCase.getId());
            if (trackerCount >= MAX_TRACKERS_PER_CASE) {
                throw new CourtierException.Conflict("This case has reached the maximum tracker limit of " + MAX_TRACKERS_PER_CASE);
            }
        } else {
            // CNR doesn't exist — create a stub, scraper will fill it later
            courtCase = Case.builder()
                    .cnrNumber(request.cnrNumber())
                    .status(Case.CaseStatus.UNKNOWN)
                    .build();
            courtCase = caseRepository.save(courtCase);
            log.info("New case stub created for CNR: {}", request.cnrNumber());
        }

        UserCase userCase = UserCase.builder()
                .user(user)
                .courtCase(courtCase)
                .active(true)
                .build();
        userCaseRepository.save(userCase);

        log.info("User {} started tracking case {}", userEmail, request.cnrNumber());

        long trackerCount = userCaseRepository.countByCourtCaseIdAndActiveTrue(courtCase.getId());
        return CaseResponse.from(courtCase, trackerCount);
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> getMyCases(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CourtierException.NotFound("User not found"));

        return caseRepository.findAllByUserId(user.getId()).stream()
                .map(c -> {
                    long count = userCaseRepository.countByCourtCaseIdAndActiveTrue(c.getId());
                    return CaseResponse.from(c, count);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CaseResponse getCase(String userEmail, String cnrNumber) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CourtierException.NotFound("User not found"));

        Case courtCase = caseRepository.findByCnrNumber(cnrNumber)
                .orElseThrow(() -> new CourtierException.NotFound("Case not found: " + cnrNumber));

        // verify this user is actually tracking this case
        if (!userCaseRepository.existsByUserIdAndCourtCaseId(user.getId(), courtCase.getId())) {
            throw new CourtierException.Forbidden("You are not tracking this case");
        }

        long trackerCount = userCaseRepository.countByCourtCaseIdAndActiveTrue(courtCase.getId());
        return CaseResponse.from(courtCase, trackerCount);
    }

    @Transactional
    public void removeCase(String userEmail, String cnrNumber) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CourtierException.NotFound("User not found"));

        Case courtCase = caseRepository.findByCnrNumber(cnrNumber)
                .orElseThrow(() -> new CourtierException.NotFound("Case not found: " + cnrNumber));

        UserCase userCase = userCaseRepository.findByUserIdAndCourtCaseId(user.getId(), courtCase.getId())
                .orElseThrow(() -> new CourtierException.NotFound("You are not tracking this case"));

        // soft delete — set active = false, don't actually delete the row
        userCase.setActive(false);
        userCaseRepository.save(userCase);

        log.info("User {} stopped tracking case {}", userEmail, cnrNumber);
    }
}