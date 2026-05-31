package com.courtier.courtier.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String CASE_UPDATED_TOPIC = "case-updated";

    @Bean
    public NewTopic caseUpdatedTopic() {
        return TopicBuilder.name(CASE_UPDATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}