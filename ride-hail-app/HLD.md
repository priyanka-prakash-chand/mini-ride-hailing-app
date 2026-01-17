# High-Level Design (HLD) Document
## Multi-Tenant, Multi-Region Ride-Hailing System

### 1. System Overview

#### 1.1 Purpose
This document describes the high-level architecture of a scalable, multi-tenant, multi-region ride-hailing system designed to handle:
- ~100,000 concurrent drivers
- ~10,000 ride requests per minute
- ~200,000 location updates per second
- Sub-second driver-rider matching (p95 < 1s)

#### 1.2 Key Requirements
- **Multi-tenancy**: Support multiple tenants with data isolation
- **Multi-region**: Deploy across multiple geographic regions
- **Real-time matching**: Match drivers to riders within 1 second (p95)
- **Dynamic pricing**: Surge pricing based on demand/supply
- **Payment processing**: Integration with external Payment Service Providers (PSPs)
- **Notifications**: Real-time notifications for ride events
- **Monitoring**: Comprehensive observability with New Relic

### 2. System Architecture

#### 2.1 High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                            │
│  (Web Frontend, Mobile Apps, API Clients)                       │
└────────────────────────────┬────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway / Load Balancer                │
│  (Rate Limiting, Authentication, Routing)                        │
└────────────────────────────┬────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Application Layer (Spring Boot)               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Ride Service │  │Driver Service│  │Payment Service│          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │Matching Svc  │  │Location Svc  │  │Notification  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└────────────────────────────┬────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   Redis      │      │    Kafka     │      │   Database   │
│  (Cache &    │      │  (Messaging) │      │ (PostgreSQL/ │
│   Locks)     │      │              │      │    MySQL)    │
└──────────────┘      └──────────────┘      └──────────────┘
                              │
                              ▼
                    ┌──────────────┐
                    │  New Relic   │
                    │ (Monitoring) │
                    └──────────────┘
```

#### 2.2 Component Breakdown

**2.2.1 API Layer**
- RESTful APIs with Spring Boot
- Stateless design for horizontal scaling
- Multi-tenant support via headers (`X-Tenant-Id`, `X-Region`)
- Rate limiting (Redis-based sliding window)
- Input validation and idempotency

**2.2.2 Business Logic Layer**
- **RideService**: Core ride lifecycle management
- **DriverMatchingService**: Spatial indexing for fast driver lookup
- **SurgePricingService**: Dynamic pricing based on demand/supply
- **FareCalculationService**: Base fare + surge calculation
- **PaymentService**: Integration with external PSPs
- **LocationUpdateService**: High-throughput location update processing
- **NotificationService**: Async event notifications

**2.2.3 Data Layer**
- **Primary Database**: PostgreSQL/MySQL for transactional data
- **Cache**: Redis for:
  - Driver location caching
  - Ride state caching
  - Idempotency keys
  - Rate limiting counters
- **Spatial Index**: Geohash-based in-memory index for driver lookups

**2.2.4 Messaging Layer**
- **Kafka**: Async event processing for:
  - Ride lifecycle events
  - Driver location updates (batched)
  - Notifications

**2.2.5 Monitoring Layer**
- **New Relic APM**: Application performance monitoring
- **Spring Actuator**: Health checks, metrics endpoints
- **Custom Metrics**: Business metrics (ride requests, matches, failures)

### 3. Data Model

#### 3.1 Core Entities

**Rider**
- ID, Name, Email, Phone
- Tenant ID, Region
- Payment preferences

**Driver**
- ID, Name, Phone, Vehicle details
- Current location (lat/lng)
- Status (ONLINE, OFFLINE, ON_TRIP)
- Rating, Tenant ID, Region

**Ride**
- Ride ID (UUID)
- Rider, Driver (nullable)
- Pickup/Dropoff locations
- Status (PENDING, MATCHED, IN_PROGRESS, COMPLETED, CANCELLED)
- Tier (ECONOMY, PREMIUM, LUXURY)
- Payment method
- Fare details (base, surge, total)
- Timestamps (requested, matched, started, completed)
- Tenant ID, Region

#### 3.2 Data Partitioning Strategy

- **Multi-tenancy**: Tenant ID in all tables, row-level isolation
- **Multi-region**: Region-based partitioning
- **Writes**: Region-local (no cross-region blocking)
- **Reads**: Can read from local region with eventual consistency

### 4. Key Algorithms & Design Patterns

#### 4.1 Driver Matching Algorithm

**Goal**: Match rider to best driver within 1s (p95)

**Approach**:
1. **Spatial Indexing**: Geohash-based index for O(1) proximity lookup
2. **Caching**: Redis cache for available drivers in region
3. **Scoring**: Distance + rating + availability
4. **Retry Logic**: Expand search radius if no match found

**Performance**:
- Spatial index lookup: ~10ms
- Database query (if needed): ~50ms
- Total: < 100ms (well under 1s requirement)

#### 4.2 Location Update Processing

**Challenge**: 200k updates/sec

**Solution**:
- **Batching**: Collect updates in memory buffer (100ms window)
- **Async Processing**: `@Async` with thread pool
- **Batch DB writes**: Hibernate batch inserts
- **Cache-first**: Update Redis cache, then DB

**Throughput**:
- Batch size: 20,000 updates per batch
- Batch frequency: 10 batches/sec = 200k updates/sec

#### 4.3 Surge Pricing

**Formula**:
```
surge_multiplier = base_multiplier + (demand / supply) * factor
```

**Factors**:
- Current demand (active ride requests)
- Available drivers in area
- Time of day
- Historical patterns

#### 4.4 State Machine

**Ride States**:
```
PENDING → MATCHED → IN_PROGRESS → COMPLETED
         ↓           ↓
      CANCELLED   CANCELLED
```

**Transitions**:
- Validated via `RideStateMachine`
- Prevents invalid state changes
- Ensures data consistency

### 5. Scalability & Performance

#### 5.1 Horizontal Scaling

- **Stateless APIs**: All services are stateless, can scale horizontally
- **Load Balancing**: Round-robin or least-connections
- **Database**: Read replicas for read-heavy operations
- **Cache**: Redis cluster for high availability

#### 5.2 Performance Optimizations

1. **Caching Strategy**:
   - Driver locations: 5s TTL
   - Ride state: 60s TTL
   - Available drivers: 10s TTL

2. **Database Optimizations**:
   - Indexes on: tenant_id, region, ride_id, driver_id, status
   - Connection pooling (HikariCP)
   - Batch inserts for location updates
   - Query optimization (avoid N+1 queries)

3. **Async Processing**:
   - Location updates: Async batch processing
   - Notifications: Kafka async messaging
   - Payment processing: Non-blocking

#### 5.3 Capacity Planning

**Assumptions**:
- 100k drivers
- 10k ride requests/min = 167 req/sec
- 200k location updates/sec

**Resource Requirements** (per region):
- **Application Servers**: 10-20 instances (auto-scaling)
- **Database**: Primary + 2 read replicas
- **Redis**: 3-node cluster
- **Kafka**: 3-broker cluster

### 6. Reliability & Fault Tolerance

#### 6.1 High Availability

- **Multi-region deployment**: Active-active or active-passive
- **Database**: Primary-replica with automatic failover
- **Redis**: Cluster mode with replication
- **Kafka**: Multi-broker cluster

#### 6.2 Data Consistency

- **ACID transactions**: For critical operations (ride creation, payment)
- **Eventual consistency**: For non-critical data (driver location)
- **Idempotency**: All write operations are idempotent
- **Distributed locks**: For atomic driver allocation

#### 6.3 Error Handling

- **Retry logic**: Exponential backoff for transient failures
- **Circuit breakers**: For external service calls (PSPs)
- **Graceful degradation**: Fallback to cached data if DB unavailable
- **Dead letter queues**: For failed Kafka messages

### 7. Security

#### 7.1 Authentication & Authorization

- **API Keys**: For service-to-service communication
- **JWT Tokens**: For user authentication (future enhancement)
- **Role-based access**: Rider, Driver, Admin roles

#### 7.2 Data Security

- **Encryption at rest**: Database encryption
- **Encryption in transit**: TLS/HTTPS
- **Tenant isolation**: Row-level security
- **Input validation**: All inputs validated and sanitized

#### 7.3 Rate Limiting

- **Per-tenant limits**: Prevent abuse
- **Per-IP limits**: DDoS protection
- **Sliding window**: Redis-based rate limiting

### 8. Monitoring & Observability

#### 8.1 Metrics

**Application Metrics**:
- API latency (p50, p95, p99)
- Request rate (requests/sec)
- Error rate (errors/sec)
- Database query latency
- Cache hit rate

**Business Metrics**:
- Ride requests per minute
- Driver matches per minute
- Matching latency
- Surge pricing multiplier
- Payment success rate

#### 8.2 Logging

- **Structured logging**: JSON format
- **Log levels**: ERROR, WARN, INFO, DEBUG
- **Correlation IDs**: Track requests across services
- **Centralized logging**: ELK stack or similar

#### 8.3 Alerting

**New Relic Alerts**:
- API latency > 1s (p95)
- Error rate > 1%
- Database query latency > 500ms
- Cache hit rate < 80%
- Matching failure rate > 5%

### 9. Deployment Architecture

#### 9.1 Multi-Region Setup

```
Region: us-east
├── Application Servers (10-20 instances)
├── Database (Primary + Replicas)
├── Redis Cluster
└── Kafka Cluster

Region: eu-west
├── Application Servers (10-20 instances)
├── Database (Primary + Replicas)
├── Redis Cluster
└── Kafka Cluster

Region: ap-south
├── Application Servers (10-20 instances)
├── Database (Primary + Replicas)
├── Redis Cluster
└── Kafka Cluster
```

#### 9.2 Data Replication

- **Synchronous**: Within region (primary-replica)
- **Asynchronous**: Cross-region (eventual consistency)
- **Conflict resolution**: Last-write-wins or custom logic

### 10. API Design

#### 10.1 Core Endpoints

**Ride Management**:
- `POST /v1/rides` - Create ride request
- `GET /v1/rides/{rideId}` - Get ride details
- `POST /v1/rides/{rideId}/start` - Start ride
- `POST /v1/rides/{rideId}/complete` - Complete ride
- `POST /v1/rides/{rideId}/cancel` - Cancel ride
- `POST /v1/rides/{rideId}/payment` - Process payment

**Driver Management**:
- `PUT /v1/drivers/{driverId}/location` - Update location
- `PUT /v1/drivers/{driverId}/status` - Update status
- `GET /v1/drivers/{driverId}` - Get driver details

**Driver Offers**:
- `POST /v1/offers/{rideId}/send` - Send offer to driver
- `POST /v1/offers/{rideId}/accept` - Accept offer
- `POST /v1/offers/{rideId}/decline` - Decline offer

#### 10.2 Request/Response Format

**Headers**:
- `X-Tenant-Id`: Tenant identifier (required)
- `X-Region`: Region identifier (required)
- `Idempotency-Key`: For idempotent requests (optional)

**Response Format**:
```json
{
  "rideId": "uuid",
  "status": "MATCHED",
  "driverId": 123,
  "totalFare": 120.50,
  ...
}
```

### 11. Future Enhancements

1. **Real-time tracking**: WebSocket for live location updates
2. **Machine learning**: Predictive surge pricing
3. **Route optimization**: Optimal route calculation
4. **Driver incentives**: Dynamic bonus calculation
5. **Advanced analytics**: Business intelligence dashboard

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Author**: Development Team

