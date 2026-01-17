package com.example.ride_hail_app.service;

/**
 * Interface for cache invalidation operations
 */
public interface ICacheInvalidationService {
    void invalidateCache(String cacheKey);
    void invalidateCacheByTag(String tag);
    Long getCacheVersion(String cacheKey);
    void invalidateDriverCaches(Long driverId, String tenantId, String region);
    void invalidateRideCaches(String rideId, String tenantId, String region);
    void invalidateSpatialIndex(String tenantId, String region, String geohash);
}

