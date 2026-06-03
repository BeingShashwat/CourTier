package com.courtier.courtier.case_.controller;

import com.courtier.courtier.case_.dto.AddCaseRequest;
import com.courtier.courtier.case_.dto.CaseResponse;
import com.courtier.courtier.case_.service.CaseService;
import com.courtier.courtier.common.config.RateLimitConfig;
import com.courtier.courtier.common.config.RateLimitService;
import com.courtier.courtier.common.exception.ApiResponse;
import com.courtier.courtier.scraper.CaptchaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;
    private final RateLimitService rateLimitService;

//    @GetMapping("/captcha")
//    public ResponseEntity<ApiResponse<CaptchaResponse>> getCaptcha(
//            @RequestParam String cnrNumber) {
//        CaptchaResponse response = caseService.getCaptcha(cnrNumber);
//        return ResponseEntity.ok(ApiResponse.ok(response));
//    }

    @PostMapping
    public ResponseEntity<ApiResponse<CaseResponse>> addCase(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddCaseRequest request) {
        rateLimitService.consume(
                "rate:addcase:" + userDetails.getUsername(),
                RateLimitConfig::addCaseConfig);
        CaseResponse response = caseService.addCase(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CaseResponse>>> getMyCases(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<CaseResponse> cases = caseService.getMyCases(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(cases));
    }

    @GetMapping("/{cnrNumber}")
    public ResponseEntity<ApiResponse<CaseResponse>> getCase(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cnrNumber) {
        CaseResponse response = caseService.getCase(userDetails.getUsername(), cnrNumber);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{cnrNumber}")
    public ResponseEntity<ApiResponse<Void>> removeCase(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cnrNumber) {
        caseService.removeCase(userDetails.getUsername(), cnrNumber);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{cnrNumber}/poll")
    public ResponseEntity<ApiResponse<CaseResponse>> pollNow(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String cnrNumber) {
        rateLimitService.consume(
                "rate:poll:" + userDetails.getUsername(),
                RateLimitConfig::pollCaseConfig);
        caseService.getCase(userDetails.getUsername(), cnrNumber);
        caseService.pollAndUpdate(userDetails.getUsername(), cnrNumber);
        CaseResponse response = caseService.getCase(userDetails.getUsername(), cnrNumber);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}