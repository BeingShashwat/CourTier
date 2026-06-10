package com.courtier.courtier.user.service;

import com.courtier.courtier.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnverifiedUserCleanupScheduler {

    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupAbandonedRegistrations() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        long deleted = userRepository.deleteByEnabledFalseAndCreatedAtBefore(cutoff);

        if (deleted > 0) {
            log.info("Deleted {} abandoned unverified users older than 24 hours", deleted);
        }
    }
}
