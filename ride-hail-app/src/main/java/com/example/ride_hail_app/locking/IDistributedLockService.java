package com.example.ride_hail_app.locking;

import java.time.Duration;

/**
 * Interface for distributed locking operations
 */
public interface IDistributedLockService {
    DistributedLockService.LockHandle acquireLock(String lockKey, Duration timeout);
    DistributedLockService.LockHandle acquireLock(String lockKey);
    boolean releaseLock(DistributedLockService.LockHandle lockHandle);
    DistributedLockService.LockHandle acquireLockWithRetry(String lockKey, Duration timeout, int maxRetries, long retryDelayMs);
}

