package com.courtier.courtier.otp.service;

import com.courtier.courtier.common.exception.CourtierException;
import com.courtier.courtier.notification.service.EmailService;
import com.courtier.courtier.otp.entity.OtpToken;
import com.courtier.courtier.otp.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void generateAndSendOtp(String email, OtpToken.OtpPurpose purpose) {
        // Clear any existing OTP for this purpose
        otpRepository.deleteByEmailAndPurpose(email, purpose);

        String otp = String.format(
                "%06d",
                random.nextInt(1_000_000)
        );
        OtpToken token = OtpToken.builder()
                .email(email)
                .otp(passwordEncoder.encode(otp))
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        otpRepository.save(token);

        String subject = "CourTier: Your Verification Code";
        String body = "Your verification code is: " + otp + "\nThis code will expire in 5 minutes.";

        emailService.sendEmail(email, subject, body);
    }

    @Transactional
    public void verifyOtp(String email, String otp, OtpToken.OtpPurpose purpose) {
        OtpToken token = otpRepository.findByEmailAndPurpose(email, purpose)
                .orElseThrow(() -> new CourtierException.BadRequest("Invalid or expired OTP"));

        if (token.isExpired()) {
            otpRepository.delete(token);
            throw new CourtierException.BadRequest("OTP has expired. Please request a new one.");
        }

        if (!passwordEncoder.matches(otp, token.getOtp())) {
            throw new CourtierException.BadRequest("Incorrect OTP");
        }

        // Clean up on success
        otpRepository.delete(token);
    }
}