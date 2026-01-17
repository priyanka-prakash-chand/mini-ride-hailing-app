# Atomicity and Consistency Implementation

## Overview
This document describes the atomicity and consistency mechanisms implemented to handle concurrent writes, prevent race conditions, and ensure data consistency under high traffic.

## 1. Distributed Locking

### Implementation
- **DistributedLockService**: Redis-based distributed locking using Redlock algorithm
- **Lock Acquisition**: Atomic SETNX operation with expiration
- **Lock Release**: Lua script for atomic unlock (prevents releasing wrong lock)

### Usage
```java
// Acquire lock for driver allocation
LockHandle lock = distributedLockService.acquireLock("driver:allocation:" + driverId);

try {
    // Critical section - only one thread can execute
    // Update driver status atomically
} finally {
    distributedLockService.releaseLock(lock);
}
```

### Benefits
- Prevents race conditions in driver allocation
- Ensures only one ride assigned per driver at a time
- Handles concurrent ride requests gracefully

## 2. Optimistic Locking

### Implementation
- **@Version** annotation on `Driver` and `Ride` entities
- Automatic version increment on updates
- `OptimisticLockingFailureException` on version mismatch

### Transaction Isolation
- **SERIALIZABLE** isolation for critical operations (driver allocation)
- **READ_COMMITTED** for read operations
- Prevents dirty reads and phantom reads

### Retry Logic
```java
@Retryable(
    value = {OptimisticLockingFailureException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
```

### Benefits
- Detects concurrent modifications
- Automatic retry on conflicts
- No deadlocks (unlike pessimistic locking)

## 3. Cache Invalidation Strategies

### Cache Versioning
- Each cache key has a version number
- Version increment invalidates all cached data
- Prevents stale data from being served

### Tag-Based Invalidation
- Related caches grouped by tags
- Single invalidation affects all related caches
- Example: Invalidate all driver caches when driver updates

### Implementation
```java
// Invalidate specific cache
cacheInvalidationService.invalidateCache("driver:" + driverId);

// Invalidate by tag (all related caches)
cacheInvalidationService.invalidateCacheByTag("drivers:" + tenantId + ":" + region);
```

### Cache Invalidation Triggers
1. **Driver Status Change**: Invalidates driver cache, available drivers list, spatial index
2. **Location Update**: Invalidates spatial index, driver cache
3. **Ride Status Change**: Invalidates ride cache, related driver caches
4. **Driver Allocation**: Invalidates all driver-related caches immediately

## 4. Atomic Driver Allocation

### Process Flow
1. **Find Candidate**: Spatial index lookup (fast, O(log n))
2. **Acquire Lock**: Distributed lock on driver (prevents double allocation)
3. **Re-fetch Driver**: Get latest state with lock held
4. **Validate Status**: Ensure driver is still ONLINE
5. **Update Atomically**: Change status to ON_TRIP in transaction
6. **Invalidate Caches**: Immediately invalidate all related caches
7. **Release Lock**: Free lock for next operation

### Transaction Boundaries
```java
@Transactional(isolation = Isolation.SERIALIZABLE)
private Driver allocateDriverAtomically(...) {
    // Atomic operation with highest isolation level
}
```

### Consistency Guarantees
- **No Double Allocation**: Distributed lock ensures exclusivity
- **Status Consistency**: Re-fetch with lock prevents stale reads
- **Cache Consistency**: Immediate invalidation prevents stale cache

## 5. Concurrent Write Handling

### Race Condition Prevention
1. **Distributed Locks**: For critical sections (driver allocation)
2. **Optimistic Locking**: For entity updates (version checking)
3. **Transaction Isolation**: SERIALIZABLE for critical operations
4. **Retry Logic**: Automatic retry on conflicts

### High Traffic Scenarios

#### Scenario 1: Multiple Rides Request Same Driver
- **Problem**: Two rides try to assign same driver
- **Solution**: Distributed lock ensures only one succeeds
- **Result**: First request gets driver, second finds alternative

#### Scenario 2: Concurrent Location Updates
- **Problem**: 200k updates/sec for same driver
- **Solution**: Batching + eventual consistency
- **Result**: Latest location eventually consistent, no blocking

#### Scenario 3: Cache Stampede
- **Problem**: Multiple requests invalidate cache simultaneously
- **Solution**: Cache versioning + distributed locks
- **Result**: Only one request rebuilds cache, others wait

## 6. Consistency Guarantees

### Strong Consistency
- **Driver Allocation**: Atomic with distributed lock
- **Ride Status Changes**: Transactional with state machine validation
- **Database Writes**: ACID transactions with proper isolation

### Eventual Consistency
- **Location Updates**: Batched, eventually consistent
- **Cache Updates**: Invalidated immediately, rebuilt on next access
- **Spatial Index**: Updated asynchronously, consistent within seconds

### Cache Consistency
- **Write-Through**: Updates database and invalidates cache
- **Cache Versioning**: Version check prevents stale data
- **Tag-Based Invalidation**: Ensures related data stays consistent

## 7. Transaction Management

### Isolation Levels
- **SERIALIZABLE**: Driver allocation (prevents all anomalies)
- **READ_COMMITTED**: Read operations (default, good performance)
- **REPEATABLE_READ**: Not used (SERIALIZABLE preferred for critical ops)

### Transaction Boundaries
- **Ride Creation**: Full transaction with driver allocation
- **Status Updates**: Individual transactions with state validation
- **Location Updates**: Async, eventual consistency

### Retry Strategy
- **Max Attempts**: 3 retries
- **Backoff**: Exponential (50ms, 100ms, 200ms)
- **Conditions**: Only on OptimisticLockingFailureException

## 8. Performance Considerations

### Lock Timeout
- **Default**: 5 seconds for driver allocation
- **Location Updates**: No locking (eventual consistency)
- **Cache Operations**: No locking (version-based)

### Cache Strategy
- **TTL**: 5 minutes for most caches
- **Spatial Index**: 5 minutes (frequent updates)
- **Available Drivers**: 30 seconds (real-time data)

### Database Optimization
- **Batch Updates**: 50 operations per batch
- **Connection Pooling**: 50 max connections
- **Query Optimization**: Indexes on all lookup fields

## 9. Monitoring and Observability

### Metrics Tracked
- **Lock Acquisition Failures**: Rate of lock conflicts
- **Optimistic Lock Failures**: Concurrent modification rate
- **Cache Hit/Miss Ratio**: Cache effectiveness
- **Transaction Retries**: Retry frequency

### Alerts
- **High Lock Contention**: > 10% lock acquisition failures
- **Frequent Retries**: > 5% operations requiring retry
- **Cache Invalidation Rate**: Unusual invalidation patterns

## 10. Best Practices

### Do's
✅ Use distributed locks for critical sections
✅ Invalidate caches immediately after updates
✅ Use optimistic locking for entity updates
✅ Retry on OptimisticLockingFailureException
✅ Use SERIALIZABLE isolation for critical operations

### Don'ts
❌ Don't hold locks for long operations
❌ Don't skip cache invalidation on updates
❌ Don't ignore OptimisticLockingFailureException
❌ Don't use pessimistic locking (causes deadlocks)
❌ Don't cache without versioning

## Summary

The system ensures atomicity and consistency through:
1. **Distributed Locking**: Prevents race conditions in driver allocation
2. **Optimistic Locking**: Handles concurrent entity updates
3. **Cache Versioning**: Ensures cache consistency
4. **Transaction Isolation**: SERIALIZABLE for critical operations
5. **Retry Logic**: Automatic recovery from conflicts
6. **Immediate Cache Invalidation**: Prevents stale data

All mechanisms work together to guarantee:
- ✅ No double driver allocation
- ✅ Consistent driver rankings
- ✅ Up-to-date cache data
- ✅ No race conditions under high traffic
- ✅ Graceful handling of concurrent writes

