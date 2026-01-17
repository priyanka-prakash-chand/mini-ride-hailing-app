package com.example.ride_hail_app.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * No-op cache invalidation service when Redis is not available
 */
public class NoOpCacheInvalidationService implements ICacheInvalidationService {

  public void invalidateCache(String cacheKey) {
    // No-op
  }

  public void invalidateCacheByTag(String tag) {
    // No-op
  }

  public Long getCacheVersion(String cacheKey) { return 0L; }

  public void invalidateDriverCaches(Long driverId, String tenantId,
                                     String region) {
    // No-op
  }

  public void invalidateRideCaches(String rideId, String tenantId,
                                   String region) {
    // No-op
  }

  public void invalidateSpatialIndex(String tenantId, String region,
                                     String geohash) {
    // No-op
  }
}
