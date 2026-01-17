package com.example.ride_hail_app.config;

import com.example.ride_hail_app.service.IdempotencyService;
import com.example.ride_hail_app.service.IIdempotencyService;
import com.example.ride_hail_app.service.NoOpIdempotencyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Configuration for IdempotencyService bean
 * Provides IdempotencyService when Redis is available, NoOpIdempotencyService otherwise
 */
@Configuration
public class IdempotencyServiceConfig {
    
    @Bean("idempotencyService")
    @ConditionalOnBean(RedisTemplate.class)
    public IIdempotencyService idempotencyService(RedisTemplate<String, String> redisTemplate) {
        return new IdempotencyService(redisTemplate);
    }
    
    @Bean("idempotencyService")
    @ConditionalOnMissingBean(RedisTemplate.class)
    public IIdempotencyService noOpIdempotencyService() {
        return new NoOpIdempotencyService();
    }
}

