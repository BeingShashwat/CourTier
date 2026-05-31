package com.courtier.courtier.common.config;

import com.courtier.courtier.common.exception.CourtierException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final ProxyManager<String> proxyManager;

    public void consume(String key, Supplier<BucketConfiguration> config) {
        try {
            Bucket bucket = proxyManager.getProxy(key, config);
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for key: {}", key);
                throw new CourtierException.TooManyRequests(
                        "Too many requests. Please slow down and try again.");
            }
        } catch (CourtierException.TooManyRequests e) {
            throw e; // rethrow 429 — this is intentional, not a Redis failure
        } catch (Exception e) {
            // Redis is down — fail open, log and continue
            log.error("Rate limiting unavailable (Redis down?), allowing request: {}", e.getMessage());
        }
    }
}