package com.example.ride_hail_app.service;

/**
 * Interface for idempotency operations
 */
public interface IIdempotencyService {
    String getCachedResponse(String idempotencyKey);
    void cacheResponse(String idempotencyKey, String response);
    boolean isDuplicate(String idempotencyKey);
    boolean acquireLock(String idempotencyKey);
    void releaseLock(String idempotencyKey);
}

