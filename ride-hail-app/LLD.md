# Low-Level Design (LLD) Document
## Multi-Tenant, Multi-Region Ride-Hailing System

### 1. Introduction

This document provides detailed low-level design specifications for the ride-hailing system, including class diagrams, sequence diagrams, database schemas, and implementation details.

### 2. Class Diagrams

#### 2.1 Core Domain Models

```
┌─────────────────────────────────────────────────────────────┐
│                         Rider                               │
├─────────────────────────────────────────────────────────────┤
│ - id: Long                                                  │
│ - name: String                                              │
│ - email: String                                              │
│ - phoneNumber: String                                        │
│ - tenantId: String                                          │
│ - region: String                                            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                         Driver                              │
├─────────────────────────────────────────────────────────────┤
│ - id: Long                                                  │
│ - name: String                                              │
│ - phoneNumber: String                                        │
│ - vehicleNumber: String                                      │
│ - vehicleModel: String                                       │
│ - status: DriverStatus                                       │
│ - currentLocation: Location                                  │
│ - rating: BigDecimal                                         │
│ - tenantId: String                                          │
│ - region: String                                            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                          Ride                               │
├─────────────────────────────────────────────────────────────┤
│ - id: Long                                                  │
│ - rideId: String (UUID)                                      │
│ - rider: Rider                                              │
│ - driver: Driver (nullable)                                 │
│ - pickupLocation: Location                                   │
│ - dropoffLocation: Location                                  │
│ - status: RideStatus                                         │
│ - tier: RideTier                                            │
│ - paymentMethod: PaymentMethod                               │
│ - baseFare: BigDecimal                                       │
│ - surgeMultiplier: BigDecimal                                │
│ - totalFare: BigDecimal                                      │
│ - requestedAt: LocalDateTime                                 │
│ - matchedAt: LocalDateTime                                   │
│ - startedAt: LocalDateTime                                  │
│ - completedAt: LocalDateTime                                 │
│ - tenantId: String                                          │
│ - region: String                                            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                        Location                             │
├─────────────────────────────────────────────────────────────┤
│ - latitude: BigDecimal                                       │
│ - longitude: BigDecimal                                      │
│ - address: String                                           │
└─────────────────────────────────────────────────────────────┘
```

#### 2.2 Service Layer

```
┌─────────────────────────────────────────────────────────────┐
│                      RideService                            │
├─────────────────────────────────────────────────────────────┤
│ + createRide(request, tenantId, region, idempotencyKey)    │
│ + getRide(rideId)                                           │
│ + startRide(rideId, driverId)                              │
│ + completeRide(rideId, driverId)                           │
│ + cancelRide(rideId, userId, role)                         │
│ + processPayment(rideId)                                    │
│ - convertToResponseDTO(ride)                                │
│ - allocateDriverAtomically(location, tenantId, region)     │
│ - saveRideWithRetry(ride)                                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  DriverMatchingService                      │
├─────────────────────────────────────────────────────────────┤
│ + findBestDriver(pickupLocation, tenantId, region)          │
│ + findBestDriverWithRetry(pickupLocation, tenantId, region)│
│ - scoreDriver(driver, pickupLocation)                      │
│ - calculateDistance(loc1, loc2)                            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  LocationUpdateService                      │
├─────────────────────────────────────────────────────────────┤
│ + updateLocation(driverId, location, tenantId, region)      │
│ - processBatchUpdates()                                     │
│ - batchUpdateLocations(updates)                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    SurgePricingService                       │
├─────────────────────────────────────────────────────────────┤
│ + calculateSurgeMultiplier(lat, lng, region)                │
│ - getDemand(lat, lng, region)                              │
│ - getSupply(lat, lng, region)                               │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  FareCalculationService                      │
├─────────────────────────────────────────────────────────────┤
│ + calculateBaseFare(pickup, dropoff, tier)                  │
│ + calculateTotalFare(baseFare, surgeMultiplier)            │
│ - getDistanceInKm(loc1, loc2)                              │
│ - getTierMultiplier(tier)                                   │
└─────────────────────────────────────────────────────────────┘
```

### 3. Sequence Diagrams

#### 3.1 Create Ride Request Flow

```
Rider          Controller        RideService    MatchingService    DriverRepository    CacheService    NotificationService
  |                 |                  |                |                  |                |                    |
  |--POST /rides--->|                  |                |                  |                |                    |
  |                 |--createRide()--->|                |                  |                |                    |
  |                 |                  |--validate()----|                  |                |                    |
  |                 |                  |--calculateSurge()                 |                |                    |
  |                 |                  |--calculateFare()                  |                |                    |
  |                 |                  |--findBestDriver()---------------->|                |                    |
  |                 |                  |                  |--query()------->|                |                    |
  |                 |                  |                  |<--driver--------|                |                    |
  |                 |                  |<--driver--------|                  |                |                    |
  |                 |                  |--save()-------->|                  |                |                    |
  |                 |                  |--cacheRide()---------------------->|                |                    |
  |                 |                  |--notify()----------------------------------------->|                    |
  |                 |<--response-------|                  |                  |                |                    |
  |<--200 OK--------|                  |                |                  |                |                    |
```

#### 3.2 Driver Location Update Flow

```
Driver         Controller        LocationService    BatchQueue    LocationUpdateService    Redis    Database
  |                 |                    |                |                  |                |          |
  |--PUT /location->|                    |                |                |                |          |
  |                 |--updateLocation()->|                |                |                |          |
  |                 |                    |--addToQueue()--->|                |                |          |
  |                 |<--202 Accepted-----|                |                |                |          |
  |                 |                    |                |                |                |          |
  |                 |                    |                |                |--processBatch()|          |
  |                 |                    |                |                |--updateCache()--------->|  |
  |                 |                    |                |                |--batchSave()---------------->|
```

#### 3.3 Driver Matching Flow

```
RideService    DriverMatchingService    SpatialIndex    CacheService    DriverRepository
  |                      |                    |                |                  |
  |--findBestDriver()--->|                    |                |                  |
  |                      |--findNearby()----->|                |                  |
  |                      |                    |--geohash()     |                  |
  |                      |<--driverIds--------|                |                  |
  |                      |--getFromCache()---------------------->|                  |
  |                      |<--cached drivers---|                |                  |
  |                      |--scoreDrivers()     |                |                  |
  |                      |--getDriver()----------------------------------------->|
  |                      |<--driver details---|                |                  |
  |<--best driver--------|                    |                |                  |
```

### 4. Database Schema

#### 4.1 Riders Table

```sql
CREATE TABLE riders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    region VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_region (tenant_id, region),
    INDEX idx_email (email)
);
```

#### 4.2 Drivers Table

```sql
CREATE TABLE drivers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    vehicle_number VARCHAR(50) NOT NULL,
    vehicle_model VARCHAR(100),
    status ENUM('ONLINE', 'OFFLINE', 'ON_TRIP') NOT NULL,
    current_latitude DECIMAL(10, 8),
    current_longitude DECIMAL(11, 8),
    current_address VARCHAR(500),
    rating DECIMAL(3, 2) DEFAULT 0.0,
    tenant_id VARCHAR(100) NOT NULL,
    region VARCHAR(50) NOT NULL,
    last_location_update TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_region (tenant_id, region),
    INDEX idx_status (status),
    INDEX idx_location (current_latitude, current_longitude),
    INDEX idx_status_tenant_region (status, tenant_id, region)
);
```

#### 4.3 Rides Table

```sql
CREATE TABLE rides (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ride_id VARCHAR(36) UNIQUE NOT NULL,
    rider_id BIGINT NOT NULL,
    driver_id BIGINT,
    pickup_latitude DECIMAL(10, 8) NOT NULL,
    pickup_longitude DECIMAL(11, 8) NOT NULL,
    pickup_address VARCHAR(500),
    dropoff_latitude DECIMAL(10, 8) NOT NULL,
    dropoff_longitude DECIMAL(11, 8) NOT NULL,
    dropoff_address VARCHAR(500),
    status ENUM('PENDING', 'MATCHED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL,
    tier ENUM('ECONOMY', 'PREMIUM', 'LUXURY') NOT NULL,
    payment_method ENUM('CARD', 'WALLET', 'CASH') NOT NULL,
    base_fare DECIMAL(10, 2) NOT NULL,
    surge_multiplier DECIMAL(4, 2) DEFAULT 1.0,
    total_fare DECIMAL(10, 2) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    matched_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    tenant_id VARCHAR(100) NOT NULL,
    region VARCHAR(50) NOT NULL,
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (rider_id) REFERENCES riders(id),
    FOREIGN KEY (driver_id) REFERENCES drivers(id),
    INDEX idx_ride_id (ride_id),
    INDEX idx_rider_id (rider_id),
    INDEX idx_driver_id (driver_id),
    INDEX idx_status (status),
    INDEX idx_tenant_region (tenant_id, region),
    INDEX idx_status_tenant_region (status, tenant_id, region),
    INDEX idx_requested_at (requested_at)
);
```

### 5. API Contracts

#### 5.1 POST /v1/rides

**Request**:
```json
{
  "riderId": 1,
  "pickupLatitude": 40.7128,
  "pickupLongitude": -74.0060,
  "pickupAddress": "New York, NY",
  "dropoffLatitude": 40.7589,
  "dropoffLongitude": -73.9851,
  "dropoffAddress": "Times Square, NY",
  "tier": "ECONOMY",
  "paymentMethod": "CARD"
}
```

**Response** (201 Created):
```json
{
  "rideId": "550e8400-e29b-41d4-a716-446655440000",
  "riderId": 1,
  "driverId": 5,
  "driverName": "John Doe",
  "driverPhoneNumber": "+1234567890",
  "vehicleNumber": "ABC-1234",
  "vehicleModel": "Toyota Camry",
  "pickupLatitude": 40.7128,
  "pickupLongitude": -74.0060,
  "pickupAddress": "New York, NY",
  "dropoffLatitude": 40.7589,
  "dropoffLongitude": -73.9851,
  "dropoffAddress": "Times Square, NY",
  "status": "MATCHED",
  "tier": "ECONOMY",
  "paymentMethod": "CARD",
  "baseFare": 100.00,
  "surgeMultiplier": 1.2,
  "totalFare": 120.00,
  "requestedAt": "2024-01-15T10:30:00",
  "matchedAt": "2024-01-15T10:30:01",
  "tenantId": "tenant1",
  "region": "us-east"
}
```

#### 5.2 GET /v1/rides/{rideId}

**Response** (200 OK):
```json
{
  "rideId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "IN_PROGRESS",
  ...
}
```

#### 5.3 POST /v1/rides/{rideId}/start

**Query Parameters**:
- `driverId` (required): Long

**Response** (200 OK):
```json
{
  "rideId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "IN_PROGRESS",
  "startedAt": "2024-01-15T10:35:00",
  ...
}
```

### 6. Caching Strategy

#### 6.1 Cache Keys

**Driver Location**:
```
driver:location:{driverId}:{tenantId}:{region}
TTL: 5 seconds
```

**Ride State**:
```
ride:{rideId}:{tenantId}:{region}
TTL: 60 seconds
```

**Available Drivers**:
```
drivers:available:{tenantId}:{region}:{geohash}
TTL: 10 seconds
```

**Idempotency**:
```
idempotency:{key}
TTL: 24 hours
```

#### 6.2 Cache Invalidation

- **Ride state changes**: Invalidate ride cache
- **Driver status changes**: Invalidate driver location and available drivers cache
- **Location updates**: Update driver location cache (don't invalidate)

### 7. Error Handling

#### 7.1 Error Response Format

```json
{
  "error": {
    "code": "RIDE_NOT_FOUND",
    "message": "Ride not found with ID: abc123",
    "timestamp": "2024-01-15T10:30:00Z",
    "path": "/v1/rides/abc123"
  }
}
```

#### 7.2 Error Codes

- `RIDE_NOT_FOUND`: Ride ID doesn't exist
- `RIDER_NOT_FOUND`: Rider ID doesn't exist
- `DRIVER_NOT_FOUND`: Driver ID doesn't exist
- `INVALID_STATE_TRANSITION`: Invalid ride state change
- `DRIVER_NOT_AUTHORIZED`: Driver not authorized for operation
- `NO_DRIVER_AVAILABLE`: No drivers available for matching
- `IDEMPOTENCY_KEY_CONFLICT`: Request already being processed
- `VALIDATION_ERROR`: Request validation failed

### 8. Configuration

#### 8.1 Application Properties

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/ridehail
spring.datasource.username=postgres
spring.datasource.password=password
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# Redis
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.timeout=2000ms
spring.cache.redis.time-to-live=60000

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer

# Async
spring.task.execution.pool.core-size=10
spring.task.execution.pool.max-size=50

# New Relic
management.newrelic.metrics.export.enabled=true
management.newrelic.metrics.export.api-key=${NEW_RELIC_API_KEY}
management.newrelic.metrics.export.account-id=${NEW_RELIC_ACCOUNT_ID}
```

### 9. Performance Considerations

#### 9.1 Database Query Optimization

1. **Use Indexes**: All foreign keys and frequently queried columns indexed
2. **Batch Inserts**: Location updates batched (20k per batch)
3. **Connection Pooling**: HikariCP with 20 max connections
4. **Query Batching**: Hibernate batch size = 50
5. **Read Replicas**: Read-heavy queries use read replicas

#### 9.2 Caching Strategy

1. **Cache-First**: Check cache before database
2. **Write-Through**: Update cache on write
3. **TTL-Based Eviction**: Automatic cache expiration
4. **Cache Warming**: Pre-populate frequently accessed data

#### 9.3 Async Processing

1. **Location Updates**: Async batch processing
2. **Notifications**: Kafka async messaging
3. **Payment Processing**: Non-blocking with callbacks

### 10. Testing Strategy

#### 10.1 Unit Tests

- **Service Layer**: Mock dependencies, test business logic
- **Controller Layer**: Mock services, test HTTP handling
- **Repository Layer**: Use in-memory database (H2)

#### 10.2 Integration Tests

- **API Tests**: Test full request/response cycle
- **Database Tests**: Test with real database (test container)
- **Cache Tests**: Test with embedded Redis

#### 10.3 Performance Tests

- **Load Testing**: Simulate 10k requests/min
- **Stress Testing**: Test system limits
- **Latency Testing**: Measure p50, p95, p99 latencies

### 11. Deployment

#### 11.1 Build Process

```bash
# Build JAR
mvn clean package

# Run tests
mvn test

# Build Docker image
docker build -t ride-hail-app:latest .
```

#### 11.2 Dockerfile

```dockerfile
FROM openjdk:21-jre-slim
WORKDIR /app
COPY target/ride-hail-app-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 11.3 Environment Variables

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/ridehail
SPRING_REDIS_HOST=redis
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
NEW_RELIC_API_KEY=your-api-key
NEW_RELIC_ACCOUNT_ID=your-account-id
```

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Author**: Development Team

