package com.example.ride_hail_app.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for cache invalidation strategies
 * Ensures cache consistency with versioning and invalidation patterns
 */
@org.springframework.boot.autoconfigure.condition.
ConditionalOnBean(RedisTemplate.class)
public class CacheInvalidationService implements ICacheInvalidationService {

  private static final String CACHE_VERSION_PREFIX = "cache:version:";
  private static final String CACHE_TAG_PREFIX = "cache:tag:";

  private final RedisTemplate<String, String> redisTemplate;
  private final ICacheService cacheService;

  public CacheInvalidationService(RedisTemplate<String, String> redisTemplate,
                                  ICacheService cacheService) {
    this.redisTemplate = redisTemplate;
    this.cacheService = cacheService;
  }

  /**
   * Invalidate cache by key with versioning
   * Increments version to invalidate all cached data
   */
  public void invalidateCache(String cacheKey) {
    try {
      String versionKey = CACHE_VERSION_PREFIX + cacheKey;
      redisTemplate.opsForValue().increment(versionKey);
      redisTemplate.expire(versionKey, Duration.ofDays(1).toSeconds(),
                           TimeUnit.SECONDS);
    } catch (Exception e) {
      // Fail silently if Redis is unavailable
    }
  }

  /**
   * Invalidate cache by tag (for related data)
   * Useful for invalidating all driver-related caches when a driver updates
   */
  public void invalidateCacheByTag(String tag) {
    try {
      String tagKey = CACHE_TAG_PREFIX + tag;
      redisTemplate.opsForValue().increment(tagKey);
      redisTemplate.expire(tagKey, Duration.ofDays(1).toSeconds(),
                           TimeUnit.SECONDS);
    } catch (Exception e) {
      // Fail silently if Redis is unavailable
    }
  }

  /**
   * Get cache version (for cache stampede prevention)
   */
  public Long getCacheVersion(String cacheKey) {
    try {
      String versionKey = CACHE_VERSION_PREFIX + cacheKey;
      String version = redisTemplate.opsForValue().get(versionKey);
      return version != null ? Long.parseLong(version) : 0L;
    } catch (Exception e) {
      return 0L; // Fail gracefully
    }
  }

  /**
   * Invalidate driver-related caches
   * Called when driver status or location changes
   */
  public void invalidateDriverCaches(Long driverId, String tenantId,
                                     String region) {
    // Invalidate specific driver cache
    cacheService.evictDriver(driverId);
    invalidateCache("driver:" + driverId);

    // Invalidate available drivers list for region
    cacheService.evictAvailableDrivers(tenantId, region);
    invalidateCache("drivers:available:" + tenantId + ":" + region);

    // Invalidate by tag for all driver-related data
    invalidateCacheByTag("driver:" + driverId);
    invalidateCacheByTag("drivers:" + tenantId + ":" + region);
  }

  /**
   * Invalidate ride-related caches
   */
  public void invalidateRideCaches(String rideId, String tenantId,
                                   String region) {
    // Invalidate specific ride cache
    cacheService.evictRide(rideId);
    invalidateCache("ride:" + rideId);

    // Invalidate by tag
    invalidateCacheByTag("ride:" + rideId);
    invalidateCacheByTag("rides:" + tenantId + ":" + region);
  }

  /**
   * Invalidate spatial index cache
   */
  public void invalidateSpatialIndex(String tenantId, String region,
                                     String geohash) {
    String key = "geohash:" + tenantId + ":" + region + ":" + geohash;
    redisTemplate.delete(key);
    invalidateCacheByTag("geohash:" + tenantId + ":" + region);
  }
}
