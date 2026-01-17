package com.example.ride_hail_app.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing region-based data partitioning
 * Ensures writes are region-local and avoids cross-region blocking
 */
@Service
public class RegionService {
    
    // Active regions (in production, load from configuration)
    private final Set<String> activeRegions = ConcurrentHashMap.newKeySet();
    
    public RegionService() {
        // Initialize with default regions
        activeRegions.add("us-east");
        activeRegions.add("us-west");
        activeRegions.add("eu-west");
        activeRegions.add("ap-south");
    }
    
    /**
     * Get region for a location (region routing)
     */
    public String getRegionForLocation(double latitude, double longitude) {
        // Simple region mapping based on coordinates
        // In production, use proper geolocation service
        
        if (latitude >= 25 && latitude <= 50 && longitude >= -125 && longitude <= -65) {
            return "us-east";
        } else if (latitude >= 25 && latitude <= 50 && longitude >= -125 && longitude <= -100) {
            return "us-west";
        } else if (latitude >= 35 && latitude <= 70 && longitude >= -10 && longitude <= 40) {
            return "eu-west";
        } else if (latitude >= 5 && latitude <= 35 && longitude >= 65 && longitude <= 100) {
            return "ap-south";
        }
        
        // Default region
        return "us-east";
    }
    
    /**
     * Validate if region is active
     */
    public boolean isRegionActive(String region) {
        return activeRegions.contains(region);
    }
    
    /**
     * Get all active regions
     */
    public Set<String> getActiveRegions() {
        return Set.copyOf(activeRegions);
    }
    
    /**
     * Check if operation should be region-local
     */
    public boolean shouldRouteToRegion(String region, String tenantId) {
        // In production, implement region affinity logic
        // For now, always route to specified region
        return isRegionActive(region);
    }
}

