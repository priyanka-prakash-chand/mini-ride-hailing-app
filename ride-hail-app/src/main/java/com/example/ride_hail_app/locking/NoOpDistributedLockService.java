package com.example.ride_hail_app.locking;

import java.time.Duration;
import java.util.UUID;

/**
 * No-op distributed lock service when Redis is not available
 */
public class NoOpDistributedLockService implements IDistributedLockService {
    private static final String LOCK_PREFIX = "lock:";
    private static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofSeconds(30);
    
    public DistributedLockService.LockHandle acquireLock(String lockKey, Duration timeout) {
        // Always return a lock handle (no-op locking)
        return new DistributedLockService.LockHandle(
            LOCK_PREFIX + lockKey, 
            UUID.randomUUID().toString(), 
            timeout
        );
    }
    
    public DistributedLockService.LockHandle acquireLock(String lockKey) {
        return acquireLock(lockKey, DEFAULT_LOCK_TIMEOUT);
    }
    
    public boolean releaseLock(DistributedLockService.LockHandle lockHandle) {
        return true; // Always succeed
    }
    
    public DistributedLockService.LockHandle acquireLockWithRetry(String lockKey, Duration timeout, int maxRetries, long retryDelayMs) {
        return acquireLock(lockKey, timeout);
    }
}

