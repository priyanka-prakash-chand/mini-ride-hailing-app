package com.example.ride_hail_app.service;

import com.example.ride_hail_app.model.Driver;
import com.example.ride_hail_app.model.Ride;
import com.example.ride_hail_app.model.RideStatus;
import com.example.ride_hail_app.repository.DriverRepository;
import com.example.ride_hail_app.repository.RideRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling driver offers and declined offers
 * Manages offer timeouts and retries
 */
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(RedisTemplate.class)
public class DriverOfferService implements IDriverOfferService {
    
    private static final String OFFER_PREFIX = "offer:";
    private static final Duration OFFER_TIMEOUT = Duration.ofSeconds(30); // 30 seconds to accept/decline
    
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final DriverMatchingService driverMatchingService;
    
    public DriverOfferService(
        RideRepository rideRepository,
        DriverRepository driverRepository,
        RedisTemplate<String, String> redisTemplate,
        DriverMatchingService driverMatchingService
    ) {
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
        this.redisTemplate = redisTemplate;
        this.driverMatchingService = driverMatchingService;
    }
    
    /**
     * Send ride offer to a driver
     * Returns true if offer was sent, false if driver already has an active offer
     */
    @Transactional
    public boolean sendOffer(String rideId, Long driverId) {
        String offerKey = OFFER_PREFIX + driverId + ":" + rideId;
        
        // Check if driver already has an active offer
        if (Boolean.TRUE.equals(redisTemplate.hasKey(offerKey))) {
            return false;
        }
        
        // Store offer with timeout
        redisTemplate.opsForValue().set(
            offerKey,
            "pending",
            OFFER_TIMEOUT.toSeconds(),
            TimeUnit.SECONDS
        );
        
        return true;
    }
    
    /**
     * Accept a ride offer
     */
    @Transactional
    public boolean acceptOffer(String rideId, Long driverId) {
        String offerKey = OFFER_PREFIX + driverId + ":" + rideId;
        
        // Check if offer exists and is still valid
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(offerKey))) {
            return false; // Offer expired or doesn't exist
        }
        
        Ride ride = rideRepository.findByRideId(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found"));
        
        if (ride.getStatus() != RideStatus.PENDING) {
            // Offer was already accepted or ride was cancelled
            redisTemplate.delete(offerKey);
            return false;
        }
        
        // Assign driver to ride
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found"));
        
        ride.setDriver(driver);
        ride.setStatus(RideStatus.MATCHED);
        ride.setMatchedAt(LocalDateTime.now());
        driver.setStatus(com.example.ride_hail_app.model.DriverStatus.ON_TRIP);
        
        rideRepository.save(ride);
        driverRepository.save(driver);
        
        // Remove offer
        redisTemplate.delete(offerKey);
        
        return true;
    }
    
    /**
     * Decline a ride offer
     */
    @Transactional
    public void declineOffer(String rideId, Long driverId) {
        String offerKey = OFFER_PREFIX + driverId + ":" + rideId;
        
        // Remove the offer
        redisTemplate.delete(offerKey);
        
        // Store declined offer to avoid immediate re-offer
        String declinedKey = "declined:" + driverId + ":" + rideId;
        redisTemplate.opsForValue().set(
            declinedKey,
            "declined",
            Duration.ofMinutes(5).toSeconds(), // Don't re-offer for 5 minutes
            TimeUnit.SECONDS
        );
    }
    
    /**
     * Check if an offer has expired
     */
    public boolean isOfferExpired(String rideId, Long driverId) {
        String offerKey = OFFER_PREFIX + driverId + ":" + rideId;
        return !Boolean.TRUE.equals(redisTemplate.hasKey(offerKey));
    }
    
    /**
     * Handle offer timeout - find alternative driver
     */
    @Transactional
    public void handleOfferTimeout(String rideId, Long driverId) {
        String offerKey = OFFER_PREFIX + driverId + ":" + rideId;
        
        // Remove expired offer
        redisTemplate.delete(offerKey);
        
        Ride ride = rideRepository.findByRideId(rideId)
            .orElse(null);
        
        if (ride != null && ride.getStatus() == RideStatus.PENDING) {
            // Try to find another driver
            Driver alternativeDriver = driverMatchingService.findBestDriverWithRetry(
                ride.getPickupLocation(),
                ride.getTenantId(),
                ride.getRegion()
            );
            
            if (alternativeDriver != null && !alternativeDriver.getId().equals(driverId)) {
                // Send offer to alternative driver
                sendOffer(rideId, alternativeDriver.getId());
            }
        }
    }
    
    /**
     * Check if driver has declined this ride recently
     */
    public boolean hasDeclinedRecently(String rideId, Long driverId) {
        String declinedKey = "declined:" + driverId + ":" + rideId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(declinedKey));
    }
}

