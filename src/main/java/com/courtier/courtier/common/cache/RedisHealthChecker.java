package com.courtier.courtier.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisHealthChecker {

    private final RedisConnectionFactory connectionFactory;
    private final CacheCircuitBreaker circuitBreaker;

    @Scheduled(fixedDelay = 30000)
    public void checkRedisHealth() {

        if (circuitBreaker.isRedisAvailable()) {
            return;
        }

        try (RedisConnection connection = connectionFactory.getConnection()) {

            String response = connection.ping();

            if ("PONG".equalsIgnoreCase(response)) {

                circuitBreaker.markRedisUp();

                log.info("Redis is back online. Circuit CLOSED.");

            }

        } catch (Exception e) {
            log.debug("Redis still unavailable.");
        }

    }

}