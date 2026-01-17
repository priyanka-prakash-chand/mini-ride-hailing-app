package com.example.ride_hail_app.service;

/**
 * No-op driver offer service when Redis is not available
 * Offers are handled synchronously without timeout management
 */
public class NoOpDriverOfferService implements IDriverOfferService {
    
    public boolean sendOffer(String rideId, Long driverId) {
        // Always allow - no Redis-based offer tracking
        return true;
    }
    
    public boolean acceptOffer(String rideId, Long driverId) {
        // Always allow - no offer validation
        return true;
    }
    
    public void declineOffer(String rideId, Long driverId) {
        // No-op
    }
    
    public boolean isOfferExpired(String rideId, Long driverId) {
        return false; // Never expired without Redis
    }
    
    public void handleOfferTimeout(String rideId, Long driverId) {
        // No-op
    }
    
    public boolean hasDeclinedRecently(String rideId, Long driverId) {
        return false; // No tracking without Redis
    }
}

