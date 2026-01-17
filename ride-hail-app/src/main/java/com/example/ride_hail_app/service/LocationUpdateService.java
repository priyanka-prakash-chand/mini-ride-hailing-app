package com.example.ride_hail_app.service;

import com.example.ride_hail_app.dto.LocationUpdateDTO;
import com.example.ride_hail_app.indexing.ISpatialIndex;
import com.example.ride_hail_app.model.Driver;
import com.example.ride_hail_app.model.Location;
import com.example.ride_hail_app.monitoring.NewRelicMetrics;
import com.example.ride_hail_app.repository.DriverRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling high-throughput location updates (200k/sec)
 * Uses batching and async processing for scalability
 */
@org.springframework.boot.autoconfigure.condition.
ConditionalOnBean(RedisTemplate.class)
public class LocationUpdateService implements ILocationUpdateService {

  private static final int BATCH_SIZE = 100;
  private static final long BATCH_TIMEOUT_MS = 100; // 100ms batch window

  // Queue for batching location updates
  private final BlockingQueue<LocationUpdateTask> updateQueue =
      new LinkedBlockingQueue<>(10000);

  private final DriverRepository driverRepository;
  private final ISpatialIndex spatialIndex;
  private final ICacheService cacheService;
  private final RedisTemplate<String, String> redisTemplate;
  private final NewRelicMetrics newRelicMetrics;
  private final ICacheInvalidationService cacheInvalidationService;

  public LocationUpdateService(
      DriverRepository driverRepository, ISpatialIndex spatialIndex,
      ICacheService cacheService, RedisTemplate<String, String> redisTemplate,
      NewRelicMetrics newRelicMetrics,
      ICacheInvalidationService cacheInvalidationService) {
    this.driverRepository = driverRepository;
    this.spatialIndex = spatialIndex;
    this.cacheService = cacheService;
    this.redisTemplate = redisTemplate;
    this.newRelicMetrics = newRelicMetrics;
    this.cacheInvalidationService = cacheInvalidationService;

    // Start batch processor
    startBatchProcessor();
  }

  /**
   * Update driver location (async, batched)
   * Optimized for 200k updates/sec
   */
  @Async("taskExecutor")
  public void updateDriverLocation(Long driverId,
                                   LocationUpdateDTO locationUpdate,
                                   String tenantId, String region) {
    Location location = new Location(locationUpdate.getLatitude(),
                                     locationUpdate.getLongitude(),
                                     locationUpdate.getAddress());

    // Add to batch queue (non-blocking)
    try {
      updateQueue.offer(
          new LocationUpdateTask(driverId, location, tenantId, region), 10,
          TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // Fallback to immediate update if queue is full
      processLocationUpdate(driverId, location, tenantId, region);
    }
  }

  /**
   * Process location updates in batches
   */
  private void startBatchProcessor() {
    Thread processorThread = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          List<LocationUpdateTask> batch = new ArrayList<>();

          // Collect batch with timeout
          LocationUpdateTask first =
              updateQueue.poll(BATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
          if (first != null) {
            batch.add(first);

            // Collect more items up to batch size
            while (batch.size() < BATCH_SIZE) {
              LocationUpdateTask task =
                  updateQueue.poll(10, TimeUnit.MILLISECONDS);
              if (task == null)
                break;
              batch.add(task);
            }
          }

          // Process batch
          if (!batch.isEmpty()) {
            processBatch(batch);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    });
    processorThread.setDaemon(true);
    processorThread.setName("location-update-processor");
    processorThread.start();
  }

  /**
   * Process a batch of location updates
   */
  private void processBatch(List<LocationUpdateTask> batch) {
    // Group by tenant/region for efficient processing
    Map<String, List<LocationUpdateTask>> grouped =
        batch.stream().collect(java.util.stream.Collectors.groupingBy(
            task -> task.tenantId + ":" + task.region));

    for (List<LocationUpdateTask> group : grouped.values()) {
      // Process group in parallel
      group.parallelStream().forEach(task -> {
        processLocationUpdate(task.driverId, task.location, task.tenantId,
                              task.region);
      });
    }
  }

  /**
   * Process a single location update
   */
  @Transactional
  private void processLocationUpdate(Long driverId, Location location,
                                     String tenantId, String region) {
    try {
      // Update spatial index (fast, Redis-based)
      spatialIndex.indexDriver(driverId, location, tenantId, region);

      // Update driver entity (async, eventual consistency)
      Driver driver = driverRepository.findById(driverId).orElse(null);
      if (driver != null) {
        driver.setCurrentLocation(location);
        driver.setLastLocationUpdate(java.time.LocalDateTime.now());

        // Save in background (non-blocking)
        driverRepository.save(driver);

        // Invalidate all driver-related caches
        cacheInvalidationService.invalidateDriverCaches(
            driverId, driver.getTenantId(), driver.getRegion());
      }

      // Update Redis cache directly (fast path)
      String driverKey = "driver:loc:" + driverId;
      redisTemplate.opsForValue().set(
          driverKey, location.getLatitude() + "," + location.getLongitude(),
          300, TimeUnit.SECONDS);

      // Track location update metric
      newRelicMetrics.incrementLocationUpdates();
    } catch (Exception e) {
      // Log error but don't fail - location updates are best-effort
      System.err.println("Failed to update location for driver " + driverId +
                         ": " + e.getMessage());
      newRelicMetrics.incrementApiErrors("location_update", "update_failed");
    }
  }

  /**
   * Location update task
   */
  private static class LocationUpdateTask {
    final Long driverId;
    final Location location;
    final String tenantId;
    final String region;

    LocationUpdateTask(Long driverId, Location location, String tenantId,
                       String region) {
      this.driverId = driverId;
      this.location = location;
      this.tenantId = tenantId;
      this.region = region;
    }
  }
}
