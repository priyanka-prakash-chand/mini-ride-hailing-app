package com.example.ride_hail_app.service;

import com.example.ride_hail_app.model.Driver;
import com.example.ride_hail_app.model.Ride;

import java.util.List;

/**
 * Interface for cache operations
 */
public interface ICacheService {
    void cacheDriver(Long driverId, Driver driver);
    Driver getCachedDriver(Long driverId);
    void evictDriver(Long driverId);
    void cacheRide(String rideId, Ride ride);
    Ride getCachedRide(String rideId);
    void evictRide(String rideId);
    void cacheAvailableDrivers(String tenantId, String region, List<Driver> drivers);
    List<Driver> getCachedAvailableDrivers(String tenantId, String region);
    void evictAvailableDrivers(String tenantId, String region);
}

