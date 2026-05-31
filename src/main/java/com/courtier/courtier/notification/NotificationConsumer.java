package com.courtier.courtier.notification;

import com.courtier.courtier.case_.entity.UserCase;
import com.courtier.courtier.case_.repository.UserCaseRepository;
import com.courtier.courtier.common.config.KafkaConfig;
import com.courtier.courtier.notification.Notification;
import com.courtier.courtier.notification.repository.NotificationRepository;
import com.courtier.courtier.notification.service.EmailService;
import com.courtier.courtier.polling.CaseUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final UserCaseRepository userCaseRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationMessageBuilder messageBuilder;
    private final EmailService emailService;

    @KafkaListener(
            topics = KafkaConfig.CASE_UPDATED_TOPIC,
            groupId = "courtier-notifications"
    )
    public void consume(CaseUpdatedEvent event) {
        log.info("Received CaseUpdatedEvent for CNR: {} changes: {}", event.cnrNumber(), event.changes());

        // Find all users tracking this case
        List<UserCase> trackers = userCaseRepository.findByCourtCaseCnrNumberAndActiveTrue(event.cnrNumber());

        for (UserCase userCase : trackers) {
            String userEmail = userCase.getUser().getEmail();

            for (CaseUpdatedEvent.ChangeType changeType : event.changes()) {
                String message = messageBuilder.build(event, changeType);

                // 1. Persist to Database for the User's Notification Inbox
                Notification notification = Notification.builder()
                        .userId(userCase.getUser().getId())
                        .cnrNumber(event.cnrNumber())
                        .changeType(changeType.name())
                        .message(message)
                        .status(Notification.NotificationStatus.SENT)
                        .build();

                notificationRepository.save(notification);

                // 2. Dispatch to the Delivery Gateway
                String subject = "CourTier Alert: Update on Case " + event.cnrNumber();
                emailService.sendEmail(userEmail, subject, message);
            }
        }
    }
}