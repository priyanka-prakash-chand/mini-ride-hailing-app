package com.example.ride_hail_app.indexing;

import com.example.ride_hail_app.model.Location;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Interface for spatial indexing operations
 */
public interface ISpatialIndex {
    String geohash(BigDecimal latitude, BigDecimal longitude);
    List<String> getNeighboringGeohashes(String geohash);
    void indexDriver(Long driverId, Location location, String tenantId, String region);
    Set<Long> findDriversInProximity(Location location, String tenantId, String region, double radiusKm);
    void removeDriver(Long driverId, String tenantId, String region);
}

