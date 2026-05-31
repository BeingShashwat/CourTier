package com.courtier.courtier.common.config;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static io.github.bucket4j.Bandwidth.builder;

@Configuration
@RequiredArgsConstructor
public class RateLimitConfig {

    private final StatefulRedisConnection<String, byte[]> lettuceConnection;

    @Bean
    public ProxyManager<String> proxyManager() {
        return LettuceBasedProxyManager.builderFor(lettuceConnection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofSeconds(10)))
                .build();
    }

    // POST /api/auth/register — 5 per hour per IP
    public static BucketConfiguration registerConfig() {
        return BucketConfiguration.builder()
                .addLimit(builder().capacity(5)
                        .refillIntervally(5, Duration.ofHours(1))
                        .build())
                .build();
    }

    // POST /api/auth/login — 10 per 15 minutes per IP
    public static BucketConfiguration loginConfig() {
        return BucketConfiguration.builder()
                .addLimit(builder().capacity(10)
                        .refillIntervally(10, Duration.ofMinutes(15))
                        .build())
                .build();
    }

    // POST /api/auth/forgot-password — 3 per hour per email
    public static BucketConfiguration forgotPasswordConfig() {
        return BucketConfiguration.builder()
                .addLimit(builder().capacity(3)
                        .refillIntervally(3, Duration.ofHours(1))
                        .build())
                .build();
    }

    // POST /api/cases — 20 per day per user
    public static BucketConfiguration addCaseConfig() {
        return BucketConfiguration.builder()
                .addLimit(builder().capacity(20)
                        .refillIntervally(20, Duration.ofDays(1))
                        .build())
                .build();
    }

    // POST /api/cases/{cnr}/poll — 30 per hour per user
    public static BucketConfiguration pollCaseConfig() {
        return BucketConfiguration.builder()
                .addLimit(builder().capacity(30)
                        .refillIntervally(30, Duration.ofHours(1))
                        .build())
                .build();
    }

    // POST /api/auth/verify-email — 10 per 15 minutes per email
    public static BucketConfiguration verifyOtpConfig() {
        return BucketConfiguration.builder()
                .addLimit(builder().capacity(10)
                        .refillIntervally(10, Duration.ofMinutes(15))
                        .build())
                .build();
    }

    // POST /api/auth/reset-password — 5 per 15 minutes per email
    public static BucketConfiguration resetPasswordConfig() {
        return BucketConfiguration.builder()
                .addLimit(builder().capacity(5)
                        .refillIntervally(5, Duration.ofMinutes(15))
                        .build())
                .build();
    }
}