package com.courtier.courtier.case_.service;

import com.courtier.courtier.case_.dto.AddCaseRequest;
import com.courtier.courtier.case_.dto.CaseResponse;
import com.courtier.courtier.case_.entity.Case;
import com.courtier.courtier.case_.entity.CaseAct;
import com.courtier.courtier.case_.entity.HearingHistory;
import com.courtier.courtier.case_.entity.UserCase;
import com.courtier.courtier.case_.repository.CaseRepository;
import com.courtier.courtier.case_.repository.UserCaseRepository;
import com.courtier.courtier.common.config.KafkaConfig;
import com.courtier.courtier.common.exception.CourtierException;
import com.courtier.courtier.polling.CaseUpdatedEvent;
import com.courtier.courtier.polling.DiffDetector;
import com.courtier.courtier.scraper.ScraperRouter;
import com.courtier.courtier.user.entity.User;
import com.courtier.courtier.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {

    private static final int MAX_TRACKERS_PER_CASE = 5;
    private static final String CASE_CACHE_PREFIX = "case:cnr:";
    private static final Duration CASE_CACHE_TTL = Duration.ofMinutes(15);

    private final CaseRepository caseRepository;
    private final UserCaseRepository userCaseRepository;
    private final UserRepository userRepository;
    private final ScraperRouter scraperRouter;
    private final DiffDetector diffDetector;
    private final KafkaTemplate<String, CaseUpdatedEvent> kafkaTemplate;
    private final RedisTemplate<String, CaseResponse> caseRedisTemplate;

    @Transactional
    public CaseResponse addCase(String userEmail, AddCaseRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CourtierException.NotFound("User not found"));

        Case courtCase = caseRepository.findByCnrNumber(request.cnrNumber())
                .orElse(null);

        if (courtCase != null) {
            Optional<UserCase> existingTracker = userCaseRepository
                    .findByUserIdAndCourtCaseId(user.getId(), courtCase.getId());

            if (existingTracker.isPresent()) {
                UserCase tracker = existingTracker.get();
                if (tracker.isActive()) {
                    throw new CourtierException.Conflict("You are already tracking this case");
                } else {
                    // Previously removed — re-activate
                    tracker.setActive(true);
                    userCaseRepository.save(tracker);
                    long trackerCount = userCaseRepository
                            .countByCourtCaseIdAndActiveTrue(courtCase.getId());
                    return CaseResponse.from(courtCase, trackerCount);
                }
            }

            long count = userCaseRepository.countByCourtCaseIdAndActiveTrue(courtCase.getId());
            if (count >= MAX_TRACKERS_PER_CASE) {
                throw new CourtierException.Conflict(
                        "This case has reached the maximum tracker limit");
            }
        } else {
            Case scraped;
            try {
                scraped = scraperRouter.scrape(request.cnrNumber());
            } catch (CourtierException e) {
                throw e;
            } catch (Exception e) {
                log.error("Scraper failed: {}", e.getMessage(), e);
                throw new CourtierException.BadRequest("Failed to fetch case details");
            }

            if (scraped == null) {
                throw new CourtierException.NotFound("CNR not found. Verify and try again.");
            }
            courtCase = caseRepository.save(scraped);
        }

        UserCase userCase = UserCase.builder()
                .user(user)
                .courtCase(courtCase)
                .active(true)
                .build();
        userCaseRepository.save(userCase);

        long trackerCount = userCaseRepository
                .countByCourtCaseIdAndActiveTrue(courtCase.getId());
        return CaseResponse.from(courtCase, trackerCount);
    }

//    public CaptchaResponse getCaptcha(String cnrNumber) {
//        try {
//            return scraperRouter.initSession(cnrNumber);
//        } catch (CourtierException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new CourtierException.BadRequest(
//                    "Failed to initialize session: " + e.getMessage());
//        }
//    }

    @Transactional(readOnly = true)
    public List<CaseResponse> getMyCases(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CourtierException.NotFound("User not found"));

        return caseRepository.findAllByUserId(user.getId()).stream()
                .map(c -> {
                    long count = userCaseRepository
                            .countByCourtCaseIdAndActiveTrue(c.getId());
                    return CaseResponse.from(c, count);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CaseResponse getCase(String userEmail, String cnrNumber) {
        log.warn("GET CASE called by {} for cnr={}", userEmail, cnrNumber);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CourtierException.NotFound("User not found"));

        Case courtCase = caseRepository.findByCnrNumber(cnrNumber)
                .orElseThrow(() -> new CourtierException.NotFound(
                        "Case not found: " + cnrNumber));

        if (!userCaseRepository.existsByUserIdAndCourtCaseId(
                user.getId(), courtCase.getId())) {
            throw new CourtierException.Forbidden("You are not tracking this case");
        }

        // Try cache — fall back to DB if Redis is down
        try {
            String cacheKey = CASE_CACHE_PREFIX + cnrNumber;
            CaseResponse cached = (CaseResponse) caseRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
//                log.debug("Case cache HIT: {}", cnrNumber);
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable during case cache read, fetching from DB: {}", e.getMessage());
        }

        long trackerCount = userCaseRepository.countByCourtCaseIdAndActiveTrue(courtCase.getId());
        CaseResponse response = CaseResponse.from(courtCase, trackerCount);

        try {
            caseRedisTemplate.opsForValue().set(CASE_CACHE_PREFIX + cnrNumber, response, CASE_CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis unavailable during case cache write, continuing without cache: {}", e.getMessage());
        }

        return response;
    }

    @Transactional
    public void removeCase(String userEmail, String cnrNumber) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CourtierException.NotFound("User not found"));

        Case courtCase = caseRepository.findByCnrNumber(cnrNumber)
                .orElseThrow(() -> new CourtierException.NotFound(
                        "Case not found: " + cnrNumber));

        UserCase userCase = userCaseRepository
                .findByUserIdAndCourtCaseId(user.getId(), courtCase.getId())
                .orElseThrow(() -> new CourtierException.NotFound(
                        "You are not tracking this case"));

        userCase.setActive(false);
        userCaseRepository.save(userCase);
        log.info("User {} stopped tracking case {}", userEmail, cnrNumber);
    }

    @Transactional
    public CaseResponse pollAndUpdate(String userEmail, String cnrNumber) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CourtierException.NotFound("User not found"));

        Case courtCase = caseRepository.findByCnrNumber(cnrNumber)
                .orElseThrow(() -> new CourtierException.NotFound("Case not found"));

        if (!userCaseRepository.existsByUserIdAndCourtCaseId(
                user.getId(), courtCase.getId())) {
            throw new CourtierException.Forbidden("You are not tracking this case");
        }

        try {
            Case updated = scraperRouter.scrape(cnrNumber);
            if (updated == null) {
                throw new CourtierException.BadRequest("CNR not found");
            }

            CaseUpdatedEvent event = diffDetector.detect(courtCase, updated);

            courtCase.setCaseType(updated.getCaseType());
            courtCase.setJudgeName(updated.getJudgeName());
            courtCase.setCaseStage(updated.getCaseStage());
            courtCase.setNextHearingDate(updated.getNextHearingDate());
            courtCase.setLastHearingDate(updated.getLastHearingDate());
            courtCase.setStatus(updated.getStatus());
            courtCase.setLastPolledAt(updated.getLastPolledAt());

            courtCase.getHearingHistory().clear();
            for (HearingHistory hearing : updated.getHearingHistory()) {
                hearing.setCourtCase(courtCase);
                courtCase.getHearingHistory().add(hearing);
            }

            courtCase.getActs().clear();
            for (CaseAct act : updated.getActs()) {
                act.setCourtCase(courtCase);
                courtCase.getActs().add(act);
            }

            caseRepository.save(courtCase);

            // Evict stale cache after update
            caseRedisTemplate.delete(CASE_CACHE_PREFIX + cnrNumber);

            if (event != null) {
                kafkaTemplate.send(KafkaConfig.CASE_UPDATED_TOPIC,
                        courtCase.getCnrNumber(), event);
                log.info("Manual Poll: Published CaseUpdatedEvent for CNR: {} changes: {}",
                        courtCase.getCnrNumber(), event.changes());
            }

        } catch (CourtierException e) {
            throw e;
        } catch (Exception e) {
            log.error("Poll failed for CNR {}: {}", cnrNumber, e.getMessage());
            throw new CourtierException.BadRequest("Scraping failed. Please try again.");
        }

        long trackerCount = userCaseRepository
                .countByCourtCaseIdAndActiveTrue(courtCase.getId());
        return CaseResponse.from(courtCase, trackerCount);
    }
}