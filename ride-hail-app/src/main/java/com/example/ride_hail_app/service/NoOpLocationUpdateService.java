package com.example.ride_hail_app.service;

import com.example.ride_hail_app.dto.LocationUpdateDTO;
import com.example.ride_hail_app.model.Driver;
import com.example.ride_hail_app.monitoring.NewRelicMetrics;
import com.example.ride_hail_app.repository.DriverRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * No-op location update service when Redis is not available
 * Updates are processed synchronously without batching
 */
public class NoOpLocationUpdateService implements ILocationUpdateService {
    
    private final DriverRepository driverRepository;
    private final NewRelicMetrics newRelicMetrics;
    
    public NoOpLocationUpdateService(
        DriverRepository driverRepository,
        NewRelicMetrics newRelicMetrics
    ) {
        this.driverRepository = driverRepository;
        this.newRelicMetrics = newRelicMetrics;
    }
    
    /**
     * Update driver location (synchronous, no batching)
     */
    @org.springframework.scheduling.annotation.Async
    public void updateDriverLocation(Long driverId, LocationUpdateDTO locationUpdate, String tenantId, String region) {
        try {
            Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + driverId));
            
            // Update location directly
            driver.setCurrentLocation(new com.example.ride_hail_app.model.Location(
                locationUpdate.getLatitude(),
                locationUpdate.getLongitude(),
                locationUpdate.getAddress()
            ));
            
            driverRepository.save(driver);
            newRelicMetrics.incrementLocationUpdates();
        } catch (Exception e) {
            // Fail gracefully
            System.err.println("Error updating driver location: " + e.getMessage());
        }
    }
}

