package com.example.ride_hail_app.service;

import com.example.ride_hail_app.indexing.ISpatialIndex;
import com.example.ride_hail_app.model.Driver;
import com.example.ride_hail_app.model.DriverStatus;
import com.example.ride_hail_app.model.Location;
import com.example.ride_hail_app.monitoring.NewRelicMetrics;
import com.example.ride_hail_app.repository.DriverRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to match drivers with ride requests
 * Optimized for sub-second matching (p95 < 1s requirement)
 * Includes caching and retry logic
 */
@Service
public class DriverMatchingService {
    
    private static final double MAX_MATCHING_DISTANCE_KM = 10.0; // Maximum distance for matching
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 500;
    
    private final DriverRepository driverRepository;
    private final ICacheService cacheService;
    private final ISpatialIndex spatialIndex;
    private final NewRelicMetrics newRelicMetrics;
    
    public DriverMatchingService(
        DriverRepository driverRepository,
        ICacheService cacheService,
        ISpatialIndex spatialIndex,
        NewRelicMetrics newRelicMetrics
    ) {
        this.driverRepository = driverRepository;
        this.cacheService = cacheService;
        this.spatialIndex = spatialIndex;
        this.newRelicMetrics = newRelicMetrics;
    }
    
    /**
     * Find the best available driver for a ride request with retry logic
     * Matching criteria:
     * 1. Driver must be ONLINE
     * 2. Driver must be in same tenant and region
     * 3. Driver must be within MAX_MATCHING_DISTANCE_KM
     * 4. Prioritize by: distance (closest first), then rating (highest first)
     * 
     * @param pickupLocation The pickup location
     * @param tenantId Tenant ID for multi-tenancy
     * @param region Region for multi-region support
     * @return Best matching driver or null if none found
     */
    @Transactional(readOnly = true)
    public Driver findBestDriver(Location pickupLocation, String tenantId, String region) {
        long startTime = System.currentTimeMillis();
        
        // Use spatial index for fast lookup (O(log n) instead of O(n))
        Set<Long> nearbyDriverIds = spatialIndex.findDriversInProximity(
            pickupLocation,
            tenantId,
            region,
            MAX_MATCHING_DISTANCE_KM
        );
        
        if (nearbyDriverIds.isEmpty()) {
            // Fallback to database query if spatial index returns empty
            return findBestDriverFromDatabase(pickupLocation, tenantId, region);
        }
        
        // Fetch driver details for nearby drivers (batch fetch)
        List<Driver> nearbyDrivers = nearbyDriverIds.stream()
            .map(driverId -> {
                // Try cache first
                Driver cached = cacheService.getCachedDriver(driverId);
                if (cached != null) {
                    return cached;
                }
                // Fetch from database
                return driverRepository.findById(driverId).orElse(null);
            })
            .filter(driver -> driver != null 
                && driver.getStatus() == DriverStatus.ONLINE
                && driver.getTenantId().equals(tenantId)
                && driver.getRegion().equals(region))
            .filter(driver -> {
                if (driver.getCurrentLocation() == null) {
                    return false;
                }
                double distance = pickupLocation.distanceTo(driver.getCurrentLocation());
                return distance <= MAX_MATCHING_DISTANCE_KM;
            })
            .sorted(Comparator
                .comparing((Driver d) -> {
                    Location loc = d.getCurrentLocation();
                    return loc != null ? 
                        pickupLocation.distanceTo(loc) : Double.MAX_VALUE;
                })
                .thenComparing(Driver::getRating, Comparator.reverseOrder())
            )
            .collect(Collectors.toList());
        
        Driver result = nearbyDrivers.isEmpty() ? null : nearbyDrivers.get(0);
        
        // Record matching latency
        long duration = System.currentTimeMillis() - startTime;
        newRelicMetrics.recordMatchingLatency(duration, region, result != null);
        
        return result;
    }
    
    /**
     * Fallback method using database query
     */
    private Driver findBestDriverFromDatabase(Location pickupLocation, String tenantId, String region) {
        List<Driver> availableDrivers = cacheService.getCachedAvailableDrivers(tenantId, region);
        
        if (availableDrivers == null || availableDrivers.isEmpty()) {
            availableDrivers = driverRepository.findAvailableDriversForMatching(
                DriverStatus.ONLINE,
                tenantId,
                region
            );
            cacheService.cacheAvailableDrivers(tenantId, region, availableDrivers);
        }
        
        if (availableDrivers.isEmpty()) {
            return null;
        }
        
        return availableDrivers.stream()
            .filter(driver -> {
                if (driver.getCurrentLocation() == null) {
                    return false;
                }
                double distance = pickupLocation.distanceTo(driver.getCurrentLocation());
                return distance <= MAX_MATCHING_DISTANCE_KM;
            })
            .sorted(Comparator
                .comparing((Driver d) -> {
                    Location loc = d.getCurrentLocation();
                    return loc != null ? 
                        pickupLocation.distanceTo(loc) : Double.MAX_VALUE;
                })
                .thenComparing(Driver::getRating, Comparator.reverseOrder())
            )
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Find best driver with retry logic for handling timeouts
     */
    public Driver findBestDriverWithRetry(Location pickupLocation, String tenantId, String region) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                Driver driver = findBestDriver(pickupLocation, tenantId, region);
                if (driver != null) {
                    return driver;
                }
                // If no driver found, wait before retry
                if (attempts < MAX_RETRY_ATTEMPTS - 1) {
                    Thread.sleep(RETRY_DELAY_MS);
                }
            } catch (Exception e) {
                lastException = e;
                // On exception, retry after delay
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
            attempts++;
        }
        
        // If all retries failed, throw the last exception or return null
        if (lastException != null) {
            throw new RuntimeException("Failed to find driver after " + MAX_RETRY_ATTEMPTS + " attempts", lastException);
        }
        
        return null;
    }
    
    /**
     * Get all available drivers within a radius
     */
    @Transactional(readOnly = true)
    public List<Driver> findAvailableDriversInRadius(
        Location location, 
        double radiusKm, 
        String tenantId, 
        String region
    ) {
        List<Driver> availableDrivers = driverRepository.findAvailableDriversForMatching(
            DriverStatus.ONLINE,
            tenantId,
            region
        );
        
        return availableDrivers.stream()
            .filter(driver -> {
                if (driver.getCurrentLocation() == null) {
                    return false;
                }
                double distance = location.distanceTo(driver.getCurrentLocation());
                return distance <= radiusKm;
            })
            .collect(Collectors.toList());
    }
}
