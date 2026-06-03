package com.courtier.courtier.polling;

import com.courtier.courtier.case_.entity.Case;
import com.courtier.courtier.case_.entity.HearingHistory;
import com.courtier.courtier.case_.entity.CaseAct;
import com.courtier.courtier.case_.repository.CaseRepository;
import com.courtier.courtier.common.config.KafkaConfig;
import com.courtier.courtier.scraper.ScraperRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PollingScheduler {

    private final CaseRepository caseRepository;
    private final ScraperRouter scraperRouter;
    private final DiffDetector diffDetector;
    private final KafkaTemplate<String, CaseUpdatedEvent> kafkaTemplate;

    @Scheduled(cron = "0 0 0/6 * * *")
    @Transactional
    public void pollAllCases() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(6);
        List<Case> casesToPoll = caseRepository.findCasesDueForPolling(threshold);
        log.info("Automated polling scheduler invoked — processing {} cases.", casesToPoll.size());

        for (Case courtCase : casesToPoll) {
            try {
                pollSingleCase(courtCase);
                Thread.sleep(2000); // Guard rails to protect IP health
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Failed automated poll cycle for CNR {}: {}", courtCase.getCnrNumber(), e.getMessage());
            }
        }
    }

    public void pollSingleCase(Case courtCase) throws Exception {
        String cnr = courtCase.getCnrNumber();

        log.debug("Executing stateless background poll for case: {}", cnr);
        Case updated = scraperRouter.scrape(cnr);

        if (updated == null) {
            log.warn("Scraper returned an empty response for CNR: {}", cnr);
            return;
        }

        CaseUpdatedEvent event = diffDetector.detect(courtCase, updated);

        // --- SAFE MERGE LOGIC ---
        if (updated.getCaseType() != null) courtCase.setCaseType(updated.getCaseType());
        if (updated.getJudgeName() != null) courtCase.setJudgeName(updated.getJudgeName());
        if (updated.getCaseStage() != null) courtCase.setCaseStage(updated.getCaseStage());
        if (updated.getNextHearingDate() != null) courtCase.setNextHearingDate(updated.getNextHearingDate());
        if (updated.getLastHearingDate() != null) courtCase.setLastHearingDate(updated.getLastHearingDate());
        if (updated.getStatus() != null) courtCase.setStatus(updated.getStatus());

        courtCase.setLastPolledAt(LocalDateTime.now());

        if (updated.getHearingHistory() != null && !updated.getHearingHistory().isEmpty()) {
            courtCase.getHearingHistory().clear();
            for (HearingHistory hearing : updated.getHearingHistory()) {
                hearing.setCourtCase(courtCase);
                courtCase.getHearingHistory().add(hearing);
            }
        }
        if (updated.getActs() != null && !updated.getActs().isEmpty()) {
            courtCase.getActs().clear();
            for (CaseAct act : updated.getActs()) {
                act.setCourtCase(courtCase);
                courtCase.getActs().add(act);
            }
        }

        caseRepository.save(courtCase);

        if (event != null) {
            kafkaTemplate.send(KafkaConfig.CASE_UPDATED_TOPIC, courtCase.getCnrNumber(), event);
            log.info("Kafka notification dispatch completed for CNR: {}", courtCase.getCnrNumber());
        }
    }
}