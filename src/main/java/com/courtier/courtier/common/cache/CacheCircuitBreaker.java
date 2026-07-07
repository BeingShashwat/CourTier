package com.courtier.courtier.common.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CacheCircuitBreaker {

    private volatile boolean redisAvailable = true;

    private volatile long lastFailureTime = 0;

    public boolean isRedisAvailable() {
        return redisAvailable;
    }

    public void markRedisDown() {

        if (!redisAvailable) {
            return;
        }

        redisAvailable = false;
        lastFailureTime = System.currentTimeMillis();

        log.warn("Redis circuit OPENED.");

    }

    public void markRedisUp() {

        redisAvailable = true;

    }

}