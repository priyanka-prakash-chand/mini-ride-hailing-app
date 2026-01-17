package com.example.ride_hail_app.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Random;

/**
 * Service to calculate dynamic surge pricing based on demand and supply
 * In a real system, this would use real-time data from Redis/Kafka
 */
@Service
public class SurgePricingService {
    
    private static final BigDecimal MIN_SURGE = BigDecimal.ONE;
    private static final BigDecimal MAX_SURGE = BigDecimal.valueOf(3.0);
    private static final double BASE_DEMAND_FACTOR = 0.5;
    
    /**
     * Calculate surge multiplier based on location, time, and demand
     * Simplified implementation - in production, this would use:
     * - Real-time driver availability
     * - Historical demand patterns
     * - Time of day, events, weather
     * - Machine learning models
     */
    public BigDecimal calculateSurgeMultiplier(
        double latitude, 
        double longitude, 
        String region
    ) {
        // Simplified: Random surge between 1.0 and 2.5 for demo
        // In production, this would query a real-time data store
        Random random = new Random();
        double surge = MIN_SURGE.doubleValue() + 
                      (MAX_SURGE.doubleValue() - MIN_SURGE.doubleValue()) * 
                      random.nextDouble() * BASE_DEMAND_FACTOR;
        
        return BigDecimal.valueOf(Math.min(surge, MAX_SURGE.doubleValue()))
                         .setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}

