package com.courtier.courtier.notification.dispatcher;

import com.courtier.courtier.common.config.KafkaConfig;
import com.courtier.courtier.polling.CaseUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "courtier.kafka.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class KafkaNotificationDispatcher implements NotificationDispatcher {

    private final KafkaTemplate<String, CaseUpdatedEvent> kafkaTemplate;

    @Override
    public void dispatch(CaseUpdatedEvent event) throws Exception {

        kafkaTemplate.send(
                KafkaConfig.CASE_UPDATED_TOPIC,
                event.cnrNumber(),
                event
        ).get();

        log.info("Published event to Kafka for CNR {}", event.cnrNumber());

    }
}