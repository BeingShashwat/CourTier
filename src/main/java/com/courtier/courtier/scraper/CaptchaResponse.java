package com.courtier.courtier.scraper;

public record CaptchaResponse(
        String sessionId,
        String captchaImageBase64
) {}