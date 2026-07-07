package com.courtier.courtier.outbox.service;

import com.courtier.courtier.outbox.entity.OutboxEvent;
import com.courtier.courtier.outbox.repository.OutboxRepository;
import com.courtier.courtier.polling.CaseUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publishCaseUpdatedEvent(String cnrNumber,
                                        CaseUpdatedEvent event) {

        try {

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType("CASE_UPDATED")
                    .aggregateKey(cnrNumber)
                    .payload(objectMapper.writeValueAsString(event))
                    .build();

            outboxRepository.save(outboxEvent);

        } catch (Exception e) {

            throw new RuntimeException("Failed to create outbox event", e);

        }

    }

}