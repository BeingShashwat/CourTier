package com.courtier.courtier.notification.strategy;

public interface EmailSenderStrategy {
    void sendEmail(String to, String subject, String body);
    String getProviderName();
}
