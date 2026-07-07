package com.courtier.courtier.notification.dispatcher;

import com.courtier.courtier.notification.service.NotificationService;
import com.courtier.courtier.polling.CaseUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "courtier.kafka.enabled",
        havingValue = "false"
)
public class DirectNotificationDispatcher implements NotificationDispatcher {

    private final NotificationService notificationService;

    @Override
    public void dispatch(CaseUpdatedEvent event) {

        log.info(
                "Kafka disabled. Processing notification directly for CNR {}",
                event.cnrNumber()
        );

        notificationService.process(event);

    }
}