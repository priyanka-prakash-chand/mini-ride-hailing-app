package com.example.ride_hail_app.service;

import com.example.ride_hail_app.model.Driver;
import com.example.ride_hail_app.model.Ride;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * No-op cache service when Redis is not available
 * All cache operations are no-ops
 */
public class NoOpCacheService implements ICacheService {

  public void cacheDriver(Long driverId, Driver driver) {
    // No-op
  }

  public Driver getCachedDriver(Long driverId) { return null; }

  public void evictDriver(Long driverId) {
    // No-op
  }

  public void cacheRide(String rideId, Ride ride) {
    // No-op
  }

  public Ride getCachedRide(String rideId) { return null; }

  public void evictRide(String rideId) {
    // No-op
  }

  public void cacheAvailableDrivers(String tenantId, String region,
                                    List<Driver> drivers) {
    // No-op
  }

  public List<Driver> getCachedAvailableDrivers(String tenantId,
                                                String region) {
    return null;
  }

  public void evictAvailableDrivers(String tenantId, String region) {
    // No-op
  }
}
