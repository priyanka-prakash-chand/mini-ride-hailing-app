package com.example.ride_hail_app.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling idempotency using Redis
 * Ensures that duplicate requests are handled gracefully
 */
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(RedisTemplate.class)
public class IdempotencyService implements IIdempotencyService {
    
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public IdempotencyService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Check if a request with this idempotency key has been processed
     * Returns the cached response if exists, null otherwise
     */
    public String getCachedResponse(String idempotencyKey) {
        try {
            String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            return null; // Fail gracefully
        }
    }
    
    /**
     * Store the response for an idempotency key
     */
    public void cacheResponse(String idempotencyKey, String response) {
        try {
            String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            redisTemplate.opsForValue().set(
                key,
                response,
                IDEMPOTENCY_TTL.toSeconds(),
                TimeUnit.SECONDS
            );
        } catch (Exception e) {
            // Fail silently if Redis is unavailable
        }
    }
    
    /**
     * Check if an idempotency key exists (for checking duplicates)
     */
    public boolean isDuplicate(String idempotencyKey) {
        try {
            String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            return false; // Fail gracefully
        }
    }
    
    /**
     * Mark a request as being processed (to prevent concurrent processing)
     * Returns true if successfully acquired, false if already being processed
     */
    public boolean acquireLock(String idempotencyKey) {
        try {
            String lockKey = IDEMPOTENCY_KEY_PREFIX + "lock:" + idempotencyKey;
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey,
                "processing",
                Duration.ofMinutes(5).toSeconds(),
                TimeUnit.SECONDS
            );
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            return true; // Fail open - allow processing
        }
    }
    
    /**
     * Release the processing lock
     */
    public void releaseLock(String idempotencyKey) {
        try {
            String lockKey = IDEMPOTENCY_KEY_PREFIX + "lock:" + idempotencyKey;
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            // Fail silently
        }
    }
}

