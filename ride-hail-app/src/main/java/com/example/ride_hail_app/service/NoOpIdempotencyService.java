package com.example.ride_hail_app.service;

/**
 * No-op idempotency service when Redis is not available
 */
public class NoOpIdempotencyService implements IIdempotencyService {
    
    public String getCachedResponse(String idempotencyKey) {
        return null;
    }
    
    public void cacheResponse(String idempotencyKey, String response) {
        // No-op
    }
    
    public boolean isDuplicate(String idempotencyKey) {
        return false;
    }
    
    public boolean acquireLock(String idempotencyKey) {
        return true; // Always allow
    }
    
    public void releaseLock(String idempotencyKey) {
        // No-op
    }
}

