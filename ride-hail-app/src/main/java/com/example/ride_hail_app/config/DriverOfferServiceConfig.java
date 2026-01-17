package com.example.ride_hail_app.config;

import com.example.ride_hail_app.repository.DriverRepository;
import com.example.ride_hail_app.repository.RideRepository;
import com.example.ride_hail_app.service.DriverMatchingService;
import com.example.ride_hail_app.service.DriverOfferService;
import com.example.ride_hail_app.service.IDriverOfferService;
import com.example.ride_hail_app.service.NoOpDriverOfferService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Configuration for DriverOfferService bean
 * Provides DriverOfferService when Redis is available, NoOpDriverOfferService otherwise
 */
@Configuration
public class DriverOfferServiceConfig {
    
    @Bean("driverOfferService")
    @ConditionalOnBean(RedisTemplate.class)
    public IDriverOfferService driverOfferService(
        RideRepository rideRepository,
        DriverRepository driverRepository,
        RedisTemplate<String, String> redisTemplate,
        DriverMatchingService driverMatchingService
    ) {
        return new DriverOfferService(
            rideRepository,
            driverRepository,
            redisTemplate,
            driverMatchingService
        );
    }
    
    @Bean("driverOfferService")
    @ConditionalOnMissingBean(RedisTemplate.class)
    public IDriverOfferService noOpDriverOfferService() {
        return new NoOpDriverOfferService();
    }
}

