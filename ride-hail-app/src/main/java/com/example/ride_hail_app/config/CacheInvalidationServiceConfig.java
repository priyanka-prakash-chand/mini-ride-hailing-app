package com.example.ride_hail_app.config;

import com.example.ride_hail_app.service.CacheInvalidationService;
import com.example.ride_hail_app.service.ICacheService;
import com.example.ride_hail_app.service.ICacheInvalidationService;
import com.example.ride_hail_app.service.NoOpCacheInvalidationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Configuration for CacheInvalidationService bean
 * Provides CacheInvalidationService when Redis is available, NoOpCacheInvalidationService otherwise
 */
@Configuration
public class CacheInvalidationServiceConfig {
    
    @Bean("cacheInvalidationService")
    @ConditionalOnBean(RedisTemplate.class)
    public ICacheInvalidationService cacheInvalidationService(
        RedisTemplate<String, String> redisTemplate,
        ICacheService cacheService
    ) {
        return new CacheInvalidationService(redisTemplate, cacheService);
    }
    
    @Bean("cacheInvalidationService")
    @ConditionalOnMissingBean(RedisTemplate.class)
    public ICacheInvalidationService noOpCacheInvalidationService() {
        return new NoOpCacheInvalidationService();
    }
}

