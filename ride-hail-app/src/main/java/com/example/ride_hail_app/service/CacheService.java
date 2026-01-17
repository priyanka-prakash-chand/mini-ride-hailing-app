package com.example.ride_hail_app.service;

import com.example.ride_hail_app.model.Driver;
import com.example.ride_hail_app.model.Ride;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for caching frequently accessed data using Redis
 */
@org.springframework.boot.autoconfigure.condition.
ConditionalOnBean(RedisTemplate.class)
public class CacheService implements ICacheService {

  private static final String DRIVER_CACHE_PREFIX = "driver:";
  private static final String RIDE_CACHE_PREFIX = "ride:";
  private static final String AVAILABLE_DRIVERS_PREFIX = "drivers:available:";
  private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

  private final RedisTemplate<String, Object> redisTemplate;

  public CacheService(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * Cache driver information
   */
  @Cacheable(value = "drivers", key = "#driverId")
  public void cacheDriver(Long driverId, Driver driver) {
    String key = DRIVER_CACHE_PREFIX + driverId;
    redisTemplate.opsForValue().set(key, driver, DEFAULT_TTL.toSeconds(),
                                    TimeUnit.SECONDS);
  }

  /**
   * Get cached driver
   */
  public Driver getCachedDriver(Long driverId) {
    String key = DRIVER_CACHE_PREFIX + driverId;
    return (Driver)redisTemplate.opsForValue().get(key);
  }

  /**
   * Evict driver from cache
   */
  @CacheEvict(value = "drivers", key = "#driverId")
  public void evictDriver(Long driverId) {
    String key = DRIVER_CACHE_PREFIX + driverId;
    redisTemplate.delete(key);
  }

  /**
   * Cache ride information
   */
  @Cacheable(value = "rides", key = "#rideId")
  public void cacheRide(String rideId, Ride ride) {
    String key = RIDE_CACHE_PREFIX + rideId;
    redisTemplate.opsForValue().set(key, ride, DEFAULT_TTL.toSeconds(),
                                    TimeUnit.SECONDS);
  }

  /**
   * Get cached ride
   */
  public Ride getCachedRide(String rideId) {
    String key = RIDE_CACHE_PREFIX + rideId;
    return (Ride)redisTemplate.opsForValue().get(key);
  }

  /**
   * Evict ride from cache
   */
  @CacheEvict(value = "rides", key = "#rideId")
  public void evictRide(String rideId) {
    String key = RIDE_CACHE_PREFIX + rideId;
    redisTemplate.delete(key);
  }

  /**
   * Cache available drivers for a region
   */
  public void cacheAvailableDrivers(String tenantId, String region,
                                    List<Driver> drivers) {
    String key = AVAILABLE_DRIVERS_PREFIX + tenantId + ":" + region;
    redisTemplate.opsForValue().set(
        key, drivers,
        Duration.ofSeconds(30).toSeconds(), // Short TTL for real-time data
        TimeUnit.SECONDS);
  }

  /**
   * Get cached available drivers
   */
  @SuppressWarnings("unchecked")
  public List<Driver> getCachedAvailableDrivers(String tenantId,
                                                String region) {
    String key = AVAILABLE_DRIVERS_PREFIX + tenantId + ":" + region;
    return (List<Driver>)redisTemplate.opsForValue().get(key);
  }

  /**
   * Evict available drivers cache
   */
  public void evictAvailableDrivers(String tenantId, String region) {
    String key = AVAILABLE_DRIVERS_PREFIX + tenantId + ":" + region;
    redisTemplate.delete(key);
  }
}
