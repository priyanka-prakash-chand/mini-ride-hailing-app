package com.example.ride_hail_app.config;

import com.example.ride_hail_app.indexing.ISpatialIndex;
import com.example.ride_hail_app.indexing.NoOpSpatialIndex;
import com.example.ride_hail_app.indexing.SpatialIndex;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Configuration for SpatialIndex bean
 * Provides SpatialIndex when Redis is available, NoOpSpatialIndex otherwise
 */
@Configuration
public class SpatialIndexConfig {
    
    @Bean("spatialIndex")
    @ConditionalOnBean(RedisTemplate.class)
    public ISpatialIndex spatialIndex(RedisTemplate<String, String> redisTemplate) {
        return new SpatialIndex(redisTemplate);
    }
    
    @Bean("spatialIndex")
    @ConditionalOnMissingBean(RedisTemplate.class)
    public ISpatialIndex noOpSpatialIndex() {
        return new NoOpSpatialIndex();
    }
}

