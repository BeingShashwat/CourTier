package com.courtier.courtier.notification;

import com.courtier.courtier.polling.CaseUpdatedEvent;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageBuilder {

    public String build(CaseUpdatedEvent event,
                        CaseUpdatedEvent.ChangeType changeType) {
        return switch (changeType) {
            case NEXT_HEARING_DATE_CHANGED ->
                    "Case %s: Next hearing date changed from %s to %s."
                            .formatted(
                                    event.cnrNumber(),
                                    event.oldNextHearingDate() != null
                                            ? event.oldNextHearingDate() : "unknown",
                                    event.newNextHearingDate() != null
                                            ? event.newNextHearingDate() : "not set"
                            );
            case NEW_HEARING_ADDED ->
                    "Case %s: New hearing(s) added on: %s."
                            .formatted(
                                    event.cnrNumber(),
                                    String.join(", ", event.newHearings())
                            );
            case JUDGE_CHANGED ->
                    "Case %s: Judge changed from '%s' to '%s'."
                            .formatted(
                                    event.cnrNumber(),
                                    event.oldJudgeName() != null
                                            ? event.oldJudgeName() : "unknown",
                                    event.newJudgeName()
                            );
        };
    }
}