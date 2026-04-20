package com.courtier.courtier.case_.dto;

import com.courtier.courtier.case_.entity.Case;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CaseResponse(
        Long id,
        String cnrNumber,
        String caseType,
        String filingNumber,
        LocalDate filingDate,
        String courtName,
        String courtNumber,
        String judgeName,
        String petitionerName,
        String respondentName,
        String status,
        LocalDate nextHearingDate,
        LocalDate lastHearingDate,
        String caseStage,
        LocalDateTime lastPolledAt,
        List<HearingHistoryDto> hearingHistory,
        List<CaseActDto> acts,
        long trackerCount
) {
    public record HearingHistoryDto(
            LocalDate hearingDate,
            String purpose,
            String judgeName
    ) {
    }

    public record CaseActDto(
            String actName,
            String section
    ) {
    }

    public static CaseResponse from(Case c, long trackerCount) {
        return new CaseResponse(
                c.getId(),
                c.getCnrNumber(),
                c.getCaseType(),
                c.getFilingNumber(),
                c.getFilingDate(),
                c.getCourtName(),
                c.getCourtNumber(),
                c.getJudgeName(),
                c.getPetitionerName(),
                c.getRespondentName(),
                c.getStatus().name(),
                c.getNextHearingDate(),
                c.getLastHearingDate(),
                c.getCaseStage(),
                c.getLastPolledAt(),
                c.getHearingHistory().stream()
                        .map(h -> new HearingHistoryDto(h.getHearingDate(), h.getPurpose(), h.getJudgeName()))
                        .toList(),
                c.getActs().stream()
                        .map(a -> new CaseActDto(a.getActName(), a.getSection()))
                        .toList(),
                trackerCount
        );
    }
}
