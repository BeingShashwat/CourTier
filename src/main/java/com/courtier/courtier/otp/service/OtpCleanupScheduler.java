package com.courtier.courtier.otp.service;

import com.courtier.courtier.otp.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OtpCleanupScheduler {

    private final OtpTokenRepository otpRepository;

    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void cleanupExpiredOtps() {
        long deleted = otpRepository.deleteExpired();

        if (deleted > 0) {
            log.info("Deleted {} expired OTPs", deleted);
        }
    }
}