package com.example.ride_hail_app.config;

import com.example.ride_hail_app.service.CacheService;
import com.example.ride_hail_app.service.ICacheService;
import com.example.ride_hail_app.service.NoOpCacheService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Configuration for CacheService bean
 * Provides CacheService when Redis is available, NoOpCacheService otherwise
 */
@Configuration
public class CacheServiceConfig {
    
    @Bean("cacheService")
    @ConditionalOnBean(RedisTemplate.class)
    public ICacheService cacheService(RedisTemplate<String, Object> redisTemplate) {
        return new CacheService(redisTemplate);
    }
    
    @Bean("cacheService")
    @ConditionalOnMissingBean(RedisTemplate.class)
    public ICacheService noOpCacheService() {
        return new NoOpCacheService();
    }
}

