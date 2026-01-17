package com.example.ride_hail_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Rate limiting configuration for API protection
 * Supports 10k ride requests/min per region
 */
@Configuration
public class RateLimitingConfig {
    
    /**
     * Rate limiter using Redis sliding window
     * Falls back to no-op if Redis is not available
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(RedisTemplate.class)
    public RateLimiter rateLimiter(RedisTemplate<String, String> redisTemplate) {
        return new RateLimiter(redisTemplate);
    }
    
    /**
     * No-op rate limiter when Redis is not available
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter noOpRateLimiter() {
        return new RateLimiter(null);
    }
    
    public static class RateLimiter {
        private final RedisTemplate<String, String> redisTemplate;
        private static final String RATE_LIMIT_PREFIX = "ratelimit:";
        
        public RateLimiter(RedisTemplate<String, String> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }
        
        /**
         * Check if request is within rate limit
         * @param key Rate limit key (e.g., "ride:us-east")
         * @param maxRequests Maximum requests allowed
         * @param windowSeconds Time window in seconds
         * @return true if allowed, false if rate limited
         */
        public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
            // If Redis is not available, allow all requests
            if (redisTemplate == null) {
                return true;
            }
            
            try {
                String redisKey = RATE_LIMIT_PREFIX + key;
                String count = redisTemplate.opsForValue().get(redisKey);
                
                if (count == null) {
                    // First request in window
                    redisTemplate.opsForValue().set(redisKey, "1", windowSeconds, TimeUnit.SECONDS);
                    return true;
                }
                
                int currentCount = Integer.parseInt(count);
                if (currentCount >= maxRequests) {
                    return false; // Rate limited
                }
                
                // Increment counter
                redisTemplate.opsForValue().increment(redisKey);
                return true;
            } catch (Exception e) {
                // If Redis fails, allow the request (fail open)
                return true;
            }
        }
        
        /**
         * Check rate limit for ride requests (10k/min per region)
         */
        public boolean isRideRequestAllowed(String region) {
            return isAllowed("ride:" + region, 10000, 60);
        }
        
        /**
         * Check rate limit for location updates (200k/sec total)
         */
        public boolean isLocationUpdateAllowed() {
            return isAllowed("location:global", 200000, 1);
        }
    }
}

