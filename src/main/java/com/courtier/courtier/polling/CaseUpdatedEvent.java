package com.courtier.courtier.polling;

import java.time.LocalDate;
import java.util.List;

public record CaseUpdatedEvent(
        String cnrNumber,
        String courtName,
        List<ChangeType> changes,
        LocalDate oldNextHearingDate,
        LocalDate newNextHearingDate,
        String oldJudgeName,
        String newJudgeName,
        List<String> newHearings  // dates of newly added hearings
) {
    public enum ChangeType {
        NEXT_HEARING_DATE_CHANGED,
        NEW_HEARING_ADDED,
        JUDGE_CHANGED
    }
}