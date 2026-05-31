package com.courtier.courtier.user.service;

import com.courtier.courtier.common.exception.CourtierException;
import com.courtier.courtier.common.security.JwtService;
import com.courtier.courtier.otp.entity.OtpToken;
import com.courtier.courtier.otp.service.OtpService;
import com.courtier.courtier.user.dto.*;
import com.courtier.courtier.user.entity.User;
import com.courtier.courtier.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService; // <-- Inject OtpService

    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CourtierException.Conflict("Email already registered");
        }
        if (request.phone() != null && userRepository.existsByPhone(request.phone())) {
            throw new CourtierException.Conflict("Phone already registered");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(User.Role.USER)
                .enabled(false) // <-- Account starts disabled
                .build();

        userRepository.save(user);

        otpService.generateAndSendOtp(user.getEmail(), OtpToken.OtpPurpose.REGISTRATION);
        log.info("Registration initiated for {}, OTP sent.", user.getEmail());

        return "OTP sent to your email. Please verify to activate your account.";
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyOtpRequest request) {
        otpService.verifyOtp(request.email(), request.otp(), OtpToken.OtpPurpose.REGISTRATION);

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CourtierException.NotFound("User not found"));

        user.setEnabled(true); // <-- Enable account
        userRepository.save(user);

        log.info("User {} successfully verified email.", user.getEmail());

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getFullName());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CourtierException.NotFound("User not found"));

        if (!user.isEnabled()) {
            throw new CourtierException.Forbidden("Account not verified. Please verify your email first.");
        }

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        log.info("User logged in: {}", user.getEmail());
        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getFullName());
    }

    @Transactional
    public String forgotPassword(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new CourtierException.NotFound("User not found");
        }
        otpService.generateAndSendOtp(email, OtpToken.OtpPurpose.PASSWORD_RESET);
        log.info("Password reset OTP requested for {}", email);
        return "Password reset OTP sent to your email.";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        otpService.verifyOtp(request.email(), request.otp(), OtpToken.OtpPurpose.PASSWORD_RESET);

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CourtierException.NotFound("User not found"));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("Password successfully reset for {}", user.getEmail());
        return "Password successfully reset. You can now login.";
    }
}