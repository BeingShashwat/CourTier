package com.courtier.courtier.outbox.service;

import com.courtier.courtier.common.config.KafkaConfig;
import com.courtier.courtier.outbox.entity.OutboxEvent;
import com.courtier.courtier.outbox.repository.OutboxRepository;
import com.courtier.courtier.polling.CaseUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, CaseUpdatedEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxRepository.findByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {

            try {

                CaseUpdatedEvent payload =
                        objectMapper.readValue(
                                event.getPayload(),
                                CaseUpdatedEvent.class
                        );

                kafkaTemplate.send(
                        KafkaConfig.CASE_UPDATED_TOPIC,
                        event.getAggregateKey(),
                        payload
                ).get();

                outboxRepository.delete(event);

                log.info("Published Outbox Event {}", event.getId());

            } catch (Exception e) {

                log.error(
                        "Failed to publish Outbox Event {}: {}",
                        event.getId(),
                        e.getMessage()
                );

                log.debug("Stacktrace", e);

            }

        }

    }

}