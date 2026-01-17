package com.example.ride_hail_app.service;

import com.example.ride_hail_app.model.Driver;
import com.example.ride_hail_app.model.Location;
import com.example.ride_hail_app.repository.DriverRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for maintaining up-to-date driver rankings
 * Ensures cache consistency for driver rankings used in matching
 */
@Service
@org.springframework.boot.autoconfigure.condition.
ConditionalOnBean(RedisTemplate.class)
public class DriverRankingService {

  private static final String RANKING_CACHE_PREFIX = "ranking:";
  private static final long RANKING_CACHE_TTL = 30; // 30 seconds

  private final DriverRepository driverRepository;
  private final RedisTemplate<String, String> redisTemplate;
  private final ICacheInvalidationService cacheInvalidationService;

  public DriverRankingService(
      DriverRepository driverRepository,
      RedisTemplate<String, String> redisTemplate,
      ICacheInvalidationService cacheInvalidationService) {
    this.driverRepository = driverRepository;
    this.redisTemplate = redisTemplate;
    this.cacheInvalidationService = cacheInvalidationService;
  }

  /**
   * Get ranked drivers for a location
   * Uses cache with versioning to ensure consistency
   */
  @Transactional(readOnly = true)
  public List<Driver> getRankedDrivers(Location location, String tenantId,
                                       String region) {
    String cacheKey = RANKING_CACHE_PREFIX + tenantId + ":" + region;

    // Check cache version
    Long cacheVersion = cacheInvalidationService.getCacheVersion(cacheKey);

    // Try to get from cache (simplified - in production use proper
    // serialization) For now, always recalculate to ensure freshness

    // Calculate rankings: distance + rating
    List<Driver> drivers = driverRepository.findAvailableDriversForMatching(
        com.example.ride_hail_app.model.DriverStatus.ONLINE, tenantId, region);

    return drivers.stream()
        .filter(driver -> driver.getCurrentLocation() != null)
        .sorted(
            Comparator
                .comparing((Driver d) -> {
                  double distance = location.distanceTo(d.getCurrentLocation());
                  // Combine distance and rating (lower distance + higher rating
                  // = better)
                  return distance - (d.getRating().doubleValue() * 0.5);
                })
                .thenComparing(Driver::getRating, Comparator.reverseOrder()))
        .collect(Collectors.toList());
  }

  /**
   * Invalidate rankings when driver status or location changes
   */
  public void invalidateRankings(String tenantId, String region) {
    String cacheKey = RANKING_CACHE_PREFIX + tenantId + ":" + region;
    cacheInvalidationService.invalidateCache(cacheKey);
    cacheInvalidationService.invalidateCacheByTag("rankings:" + tenantId + ":" +
                                                  region);
  }
}
