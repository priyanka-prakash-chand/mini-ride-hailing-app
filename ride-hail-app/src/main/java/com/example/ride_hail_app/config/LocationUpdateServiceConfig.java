package com.example.ride_hail_app.config;

import com.example.ride_hail_app.indexing.ISpatialIndex;
import com.example.ride_hail_app.monitoring.NewRelicMetrics;
import com.example.ride_hail_app.repository.DriverRepository;
import com.example.ride_hail_app.service.ICacheInvalidationService;
import com.example.ride_hail_app.service.ICacheService;
import com.example.ride_hail_app.service.ILocationUpdateService;
import com.example.ride_hail_app.service.LocationUpdateService;
import com.example.ride_hail_app.service.NoOpLocationUpdateService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Configuration for LocationUpdateService bean
 * Provides LocationUpdateService when Redis is available, NoOpLocationUpdateService otherwise
 */
@Configuration
public class LocationUpdateServiceConfig {
    
    @Bean("locationUpdateService")
    @ConditionalOnBean(RedisTemplate.class)
    public ILocationUpdateService locationUpdateService(
        DriverRepository driverRepository,
        ISpatialIndex spatialIndex,
        ICacheService cacheService,
        RedisTemplate<String, String> redisTemplate,
        NewRelicMetrics newRelicMetrics,
        ICacheInvalidationService cacheInvalidationService
    ) {
        return new LocationUpdateService(
            driverRepository,
            spatialIndex,
            cacheService,
            redisTemplate,
            newRelicMetrics,
            cacheInvalidationService
        );
    }
    
    @Bean("locationUpdateService")
    @ConditionalOnMissingBean(RedisTemplate.class)
    public ILocationUpdateService noOpLocationUpdateService(
        DriverRepository driverRepository,
        NewRelicMetrics newRelicMetrics
    ) {
        return new NoOpLocationUpdateService(driverRepository, newRelicMetrics);
    }
}

