package com.courtier.courtier.notification.service;

import com.courtier.courtier.notification.strategy.EmailSenderStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private final Map<String, EmailSenderStrategy> strategies;
    private final String activeProvider;

    public EmailService(List<EmailSenderStrategy> strategyList,
                        @Value("${courtier.email.provider:smtp}") String activeProvider) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(EmailSenderStrategy::getProviderName, Function.identity()));
        this.activeProvider = activeProvider;
    }

    public void sendEmail(String to, String subject, String body) {
        EmailSenderStrategy strategy = strategies.get(activeProvider);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported email provider: " + activeProvider);
        }
        strategy.sendEmail(to, subject, body);
    }
}