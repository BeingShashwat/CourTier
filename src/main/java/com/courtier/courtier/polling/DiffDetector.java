package com.courtier.courtier.polling;

import com.courtier.courtier.case_.entity.Case;
import com.courtier.courtier.case_.entity.HearingHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DiffDetector {

    /**
     * Compares old and new case state.
     * Returns a CaseUpdatedEvent if any tracked changes found, null otherwise.
     */
    public CaseUpdatedEvent detect(Case oldCase, Case newCase) {
        List<CaseUpdatedEvent.ChangeType> changes = new ArrayList<>();
        List<String> newHearings = new ArrayList<>();

        // 1 — next hearing date changed
        if (!Objects.equals(oldCase.getNextHearingDate(),
                newCase.getNextHearingDate())) {
            changes.add(CaseUpdatedEvent.ChangeType.NEXT_HEARING_DATE_CHANGED);
            log.info("CNR {}: next hearing date changed from {} to {}",
                    oldCase.getCnrNumber(),
                    oldCase.getNextHearingDate(),
                    newCase.getNextHearingDate());
        }

        // 2 — new hearings added
        Set<String> oldHearingDates = oldCase.getHearingHistory().stream()
                .map(h -> h.getHearingDate().toString())
                .collect(Collectors.toSet());

        for (HearingHistory hearing : newCase.getHearingHistory()) {
            String dateStr = hearing.getHearingDate().toString();
            if (!oldHearingDates.contains(dateStr)) {
                newHearings.add(dateStr);
                log.info("CNR {}: new hearing found on {}",
                        oldCase.getCnrNumber(), dateStr);
            }
        }
        if (!newHearings.isEmpty()) {
            changes.add(CaseUpdatedEvent.ChangeType.NEW_HEARING_ADDED);
        }

        // 3 — judge changed
        if (!Objects.equals(oldCase.getJudgeName(), newCase.getJudgeName())
                && newCase.getJudgeName() != null
                && !newCase.getJudgeName().isBlank()
                && !"---".equals(newCase.getJudgeName())) {
            changes.add(CaseUpdatedEvent.ChangeType.JUDGE_CHANGED);
            log.info("CNR {}: judge changed from '{}' to '{}'",
                    oldCase.getCnrNumber(),
                    oldCase.getJudgeName(),
                    newCase.getJudgeName());
        }

        if (changes.isEmpty()) {
            return null; // no meaningful change
        }

        return new CaseUpdatedEvent(
                oldCase.getCnrNumber(),
                oldCase.getCourtName(),
                changes,
                oldCase.getNextHearingDate(),
                newCase.getNextHearingDate(),
                oldCase.getJudgeName(),
                newCase.getJudgeName(),
                newHearings
        );
    }
}