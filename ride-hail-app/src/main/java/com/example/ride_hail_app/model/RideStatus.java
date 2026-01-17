package com.example.ride_hail_app.model;

public enum RideStatus {
    PENDING,        // Ride requested, waiting for driver
    MATCHED,        // Driver assigned
    DRIVER_ARRIVING,// Driver is on the way to pickup
    IN_PROGRESS,    // Ride started, passenger in vehicle
    PAUSED,         // Ride paused (e.g., driver stopped)
    COMPLETED,      // Ride completed
    CANCELLED       // Ride cancelled
}

