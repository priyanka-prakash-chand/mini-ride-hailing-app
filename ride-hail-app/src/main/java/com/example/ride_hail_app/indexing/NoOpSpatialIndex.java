package com.example.ride_hail_app.indexing;

import com.example.ride_hail_app.model.Location;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * No-op spatial index when Redis is not available
 * Falls back to database queries
 */
public class NoOpSpatialIndex implements ISpatialIndex {
    
    public String geohash(BigDecimal latitude, BigDecimal longitude) {
        // Return a dummy geohash
        return "0000000";
    }
    
    public List<String> getNeighboringGeohashes(String geohash) {
        return Collections.singletonList(geohash);
    }
    
    public void indexDriver(Long driverId, Location location, String tenantId, String region) {
        // No-op - will fall back to database queries
    }
    
    public Set<Long> findDriversInProximity(Location location, String tenantId, String region, double radiusKm) {
        // Return empty set - will fall back to database queries
        return new HashSet<>();
    }
    
    public void removeDriver(Long driverId, String tenantId, String region) {
        // No-op
    }
}

