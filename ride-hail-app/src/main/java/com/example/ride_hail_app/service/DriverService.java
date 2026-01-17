package com.example.ride_hail_app.service;

import com.example.ride_hail_app.dto.LocationUpdateDTO;
import com.example.ride_hail_app.model.Driver;
import com.example.ride_hail_app.model.DriverStatus;
import com.example.ride_hail_app.model.Location;
import com.example.ride_hail_app.repository.DriverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for driver management
 */
@Service
public class DriverService {
    
    private final DriverRepository driverRepository;
    private final ILocationUpdateService locationUpdateService;
    
    public DriverService(
        DriverRepository driverRepository,
        ILocationUpdateService locationUpdateService
    ) {
        this.driverRepository = driverRepository;
        this.locationUpdateService = locationUpdateService;
    }
    
    /**
     * Update driver location (for real-time tracking - 200k updates/sec)
     * Uses async batching for high throughput
     */
    public void updateDriverLocation(Long driverId, LocationUpdateDTO locationUpdate, String tenantId, String region) {
        // Use async batch processing service
        locationUpdateService.updateDriverLocation(driverId, locationUpdate, tenantId, region);
    }
    
    /**
     * Update driver status
     */
    @Transactional
    public void updateDriverStatus(Long driverId, DriverStatus status) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + driverId));
        
        driver.setStatus(status);
        driverRepository.save(driver);
    }
    
    /**
     * Get driver by ID
     */
    @Transactional(readOnly = true)
    public Driver getDriver(Long driverId) {
        return driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + driverId));
    }
}

