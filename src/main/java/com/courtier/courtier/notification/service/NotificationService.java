package com.courtier.courtier.notification.service;

import com.courtier.courtier.case_.entity.UserCase;
import com.courtier.courtier.case_.repository.UserCaseRepository;
import com.courtier.courtier.notification.Notification;
import com.courtier.courtier.notification.NotificationMessageBuilder;
import com.courtier.courtier.notification.repository.NotificationRepository;
import com.courtier.courtier.polling.CaseUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final UserCaseRepository userCaseRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationMessageBuilder messageBuilder;
    private final EmailService emailService;

    public void process(CaseUpdatedEvent event) {

        log.info(
                "Processing notification for CNR: {}",
                event.cnrNumber()
        );

        List<UserCase> trackers =
                userCaseRepository.findByCourtCaseCnrNumberAndActiveTrue(
                        event.cnrNumber()
                );

        for (UserCase userCase : trackers) {

            String userEmail = userCase.getUser().getEmail();

            for (CaseUpdatedEvent.ChangeType changeType : event.changes()) {

                String message =
                        messageBuilder.build(event, changeType);

                Notification notification =
                        Notification.builder()
                                .userId(userCase.getUser().getId())
                                .cnrNumber(event.cnrNumber())
                                .changeType(changeType.name())
                                .message(message)
                                .status(Notification.NotificationStatus.SENT)
                                .build();

                notificationRepository.save(notification);

                String subject =
                        "CourTier Alert: Update on Case "
                                + event.cnrNumber();

                emailService.sendEmail(
                        userEmail,
                        subject,
                        message
                );
            }
        }
    }
}