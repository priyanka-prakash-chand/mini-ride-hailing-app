package com.example.ride_hail_app.service;

/**
 * Interface for driver offer operations
 */
public interface IDriverOfferService {
    boolean sendOffer(String rideId, Long driverId);
    boolean acceptOffer(String rideId, Long driverId);
    void declineOffer(String rideId, Long driverId);
    boolean isOfferExpired(String rideId, Long driverId);
    void handleOfferTimeout(String rideId, Long driverId);
    boolean hasDeclinedRecently(String rideId, Long driverId);
}

