package com.courtier.courtier.notification;

import com.courtier.courtier.common.config.KafkaConfig;
import com.courtier.courtier.notification.service.NotificationService;
import com.courtier.courtier.polling.CaseUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "courtier.kafka.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = KafkaConfig.CASE_UPDATED_TOPIC,
            groupId = "courtier-notifications"
    )
    public void consume(CaseUpdatedEvent event) {

        log.info("Received Kafka event for {}", event.cnrNumber());

        notificationService.process(event);

    }
}