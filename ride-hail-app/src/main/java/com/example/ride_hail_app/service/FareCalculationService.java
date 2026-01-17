package com.example.ride_hail_app.service;

import com.example.ride_hail_app.model.Location;
import com.example.ride_hail_app.model.RideTier;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service to calculate ride fares based on distance, tier, and surge
 */
@Service
public class FareCalculationService {
    
    // Base fare per tier (in local currency units)
    private static final BigDecimal ECONOMY_BASE_FARE = new BigDecimal("50");
    private static final BigDecimal COMFORT_BASE_FARE = new BigDecimal("75");
    private static final BigDecimal PREMIUM_BASE_FARE = new BigDecimal("100");
    private static final BigDecimal LUXURY_BASE_FARE = new BigDecimal("150");
    
    // Per kilometer rate
    private static final BigDecimal ECONOMY_PER_KM = new BigDecimal("10");
    private static final BigDecimal COMFORT_PER_KM = new BigDecimal("15");
    private static final BigDecimal PREMIUM_PER_KM = new BigDecimal("20");
    private static final BigDecimal LUXURY_PER_KM = new BigDecimal("30");
    
    /**
     * Calculate base fare based on distance and tier
     */
    public BigDecimal calculateBaseFare(Location pickup, Location dropoff, RideTier tier) {
        double distanceKm = pickup.distanceTo(dropoff);
        
        BigDecimal baseFare;
        BigDecimal perKmRate;
        
        switch (tier) {
            case ECONOMY:
                baseFare = ECONOMY_BASE_FARE;
                perKmRate = ECONOMY_PER_KM;
                break;
            case COMFORT:
                baseFare = COMFORT_BASE_FARE;
                perKmRate = COMFORT_PER_KM;
                break;
            case PREMIUM:
                baseFare = PREMIUM_BASE_FARE;
                perKmRate = PREMIUM_PER_KM;
                break;
            case LUXURY:
                baseFare = LUXURY_BASE_FARE;
                perKmRate = LUXURY_PER_KM;
                break;
            default:
                baseFare = ECONOMY_BASE_FARE;
                perKmRate = ECONOMY_PER_KM;
        }
        
        BigDecimal distanceFare = perKmRate.multiply(BigDecimal.valueOf(distanceKm));
        return baseFare.add(distanceFare).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate total fare with surge multiplier
     */
    public BigDecimal calculateTotalFare(BigDecimal baseFare, BigDecimal surgeMultiplier) {
        return baseFare.multiply(surgeMultiplier)
                      .setScale(2, RoundingMode.HALF_UP);
    }
}

