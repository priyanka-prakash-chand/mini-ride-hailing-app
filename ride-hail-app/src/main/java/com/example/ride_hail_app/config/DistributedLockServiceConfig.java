package com.example.ride_hail_app.config;

import com.example.ride_hail_app.locking.DistributedLockService;
import com.example.ride_hail_app.locking.IDistributedLockService;
import com.example.ride_hail_app.locking.NoOpDistributedLockService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Configuration for DistributedLockService bean
 * Provides DistributedLockService when Redis is available, NoOpDistributedLockService otherwise
 */
@Configuration
public class DistributedLockServiceConfig {
    
    @Bean("distributedLockService")
    @ConditionalOnBean(RedisTemplate.class)
    public IDistributedLockService distributedLockService(RedisTemplate<String, String> redisTemplate) {
        return new DistributedLockService(redisTemplate);
    }
    
    @Bean("distributedLockService")
    @ConditionalOnMissingBean(RedisTemplate.class)
    public IDistributedLockService noOpDistributedLockService() {
        return new NoOpDistributedLockService();
    }
}

