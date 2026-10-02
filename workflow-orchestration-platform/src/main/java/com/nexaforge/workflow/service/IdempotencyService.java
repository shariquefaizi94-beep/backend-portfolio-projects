package com.nexaforge.workflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Provides idempotency checks using Redis as the deduplication store.
 *
 * <p>Uses a simple set-if-absent pattern with a TTL:
 * <ol>
 *   <li>Before processing a command, call {@link #tryAcquire(String)}</li>
 *   <li>If it returns {@code true}, proceed with processing</li>
 *   <li>If it returns {@code false}, the command has already been processed</li>
 * </ol>
 *
 * <p>The TTL ensures keys don't accumulate indefinitely while providing
 * a wide enough window to catch duplicate deliveries from Kafka.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String KEY_PREFIX = "idempotency:";

    private final StringRedisTemplate redisTemplate;
    private final Duration cacheTtl;

    public IdempotencyService(
            StringRedisTemplate redisTemplate,
            @Value("${workflow.idempotency.cache-ttl-minutes:60}") int ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.cacheTtl = Duration.ofMinutes(ttlMinutes);
    }

    /**
     * Attempts to acquire an idempotency lock for the given key.
     *
     * @param idempotencyKey unique identifier for the command/event
     * @return true if this is the first time seeing this key; false if duplicate
     */
    public boolean tryAcquire(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", cacheTtl);

        if (Boolean.TRUE.equals(result)) {
            log.debug("Idempotency key acquired: {}", idempotencyKey);
            return true;
        }

        log.info("Duplicate command detected, skipping: {}", idempotencyKey);
        return false;
    }

    /**
     * Releases an idempotency key, allowing re-processing.
     * Used when processing fails and should be retried.
     */
    public void release(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;
        redisTemplate.delete(key);
        log.debug("Idempotency key released: {}", idempotencyKey);
    }
}
