package com.example.ride_hail_app.service;

import com.example.ride_hail_app.dto.LocationUpdateDTO;

/**
 * Interface for location update operations
 */
public interface ILocationUpdateService {
    void updateDriverLocation(Long driverId, LocationUpdateDTO locationUpdate, String tenantId, String region);
}

