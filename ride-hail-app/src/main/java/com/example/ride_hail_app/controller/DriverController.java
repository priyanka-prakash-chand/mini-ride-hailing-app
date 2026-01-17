package com.example.ride_hail_app.controller;

import com.example.ride_hail_app.dto.LocationUpdateDTO;
import com.example.ride_hail_app.model.Driver;
import com.example.ride_hail_app.model.DriverStatus;
import com.example.ride_hail_app.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/drivers")
public class DriverController {
    
    private final DriverService driverService;
    
    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }
    
    /**
     * PUT /v1/drivers/{driverId}/location - Update driver location
     * For real-time tracking (200k updates/sec with batching)
     */
    @PutMapping("/{driverId}/location")
    public ResponseEntity<Map<String, String>> updateLocation(
        @PathVariable Long driverId,
        @Valid @RequestBody LocationUpdateDTO locationUpdate,
        @RequestHeader(value = "X-Tenant-Id", required = false, defaultValue = "default") String tenantId,
        @RequestHeader(value = "X-Region", required = false, defaultValue = "us-east") String region
    ) {
        driverService.updateDriverLocation(driverId, locationUpdate, tenantId, region);
        return ResponseEntity.accepted().body(Map.of(
            "message", "Driver location update queued",
            "driverId", driverId.toString()
        ));
    }
    
    /**
     * PUT /v1/drivers/{driverId}/status - Update driver status
     */
    @PutMapping("/{driverId}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
        @PathVariable Long driverId,
        @RequestParam DriverStatus status
    ) {
        driverService.updateDriverStatus(driverId, status);
        return ResponseEntity.ok(Map.of(
            "message", "Driver status updated successfully",
            "driverId", driverId.toString(),
            "status", status.toString()
        ));
    }
    
    /**
     * GET /v1/drivers/{driverId} - Get driver details
     */
    @GetMapping("/{driverId}")
    public ResponseEntity<Driver> getDriver(@PathVariable Long driverId) {
        Driver driver = driverService.getDriver(driverId);
        return ResponseEntity.ok(driver);
    }
}

