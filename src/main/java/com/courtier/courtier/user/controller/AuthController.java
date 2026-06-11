package com.courtier.courtier.user.controller;

import com.courtier.courtier.common.config.RateLimitConfig;
import com.courtier.courtier.common.config.RateLimitService;
import com.courtier.courtier.common.exception.ApiResponse;
import com.courtier.courtier.user.dto.*;
import com.courtier.courtier.user.service.AuthService;
import com.courtier.courtier.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final RateLimitService rateLimitService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        rateLimitService.consume(
                "rate:register:" + getClientIp(httpRequest),
                RateLimitConfig::registerConfig);
        String response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(
            @Valid @RequestBody VerifyOtpRequest request) {
        rateLimitService.consume(
                "rate:verify:" + request.email().toLowerCase(),
                RateLimitConfig::verifyOtpConfig);
        AuthResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        rateLimitService.consume(
                "rate:login:" + getClientIp(httpRequest),
                RateLimitConfig::loginConfig);
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @RequestParam String email,
            HttpServletRequest httpRequest) {
        rateLimitService.consume(
                "rate:forgot:" + email.toLowerCase(),
                RateLimitConfig::forgotPasswordConfig);
        String response = authService.forgotPassword(email);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        rateLimitService.consume(
                "rate:reset:" + request.email().toLowerCase(),
                RateLimitConfig::resetPasswordConfig);
        String response = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteMyAccount(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}