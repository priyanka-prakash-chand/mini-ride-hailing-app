# Ride Hailing Application

A scalable, multi-tenant, multi-region ride-hailing system built with Spring Boot, designed to handle high throughput and low latency requirements.

## Features

- **Multi-tenant & Multi-region**: Support for multiple tenants and geographic regions
- **Real-time Driver Matching**: Sub-second driver-rider matching (p95 < 1s)
- **Dynamic Surge Pricing**: Demand-based pricing calculation
- **High-Throughput Location Updates**: Handle 200k location updates/sec
- **Payment Integration**: Support for external Payment Service Providers (PSPs)
- **Real-time Notifications**: Async event notifications via Kafka
- **Comprehensive Monitoring**: New Relic APM integration
- **Scalable Architecture**: Stateless design for horizontal scaling

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 21
- **Database**: PostgreSQL/MySQL (H2 for development)
- **Cache**: Redis
- **Messaging**: Apache Kafka
- **Monitoring**: New Relic APM
- **Build Tool**: Maven

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- Redis (for caching and rate limiting)
- Kafka (for async messaging)
- PostgreSQL/MySQL (for production) or H2 (for development)

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd ride-hail-app
```

### 2. Configure Application

Update `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver

# Redis (optional for development)
spring.redis.host=localhost
spring.redis.port=6379

# Kafka (optional for development)
spring.kafka.bootstrap-servers=localhost:9092

# New Relic (optional)
management.newrelic.metrics.export.enabled=false
```

### 3. Build and Run

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Access the Frontend

Open your browser and navigate to:
```
http://localhost:8080
```

## API Endpoints

### Ride Management

- `POST /v1/rides` - Create a ride request
- `GET /v1/rides/{rideId}` - Get ride details
- `POST /v1/rides/{rideId}/start` - Start a ride
- `POST /v1/rides/{rideId}/complete` - Complete a ride
- `POST /v1/rides/{rideId}/cancel` - Cancel a ride
- `POST /v1/rides/{rideId}/payment` - Process payment

### Driver Management

- `PUT /v1/drivers/{driverId}/location` - Update driver location
- `PUT /v1/drivers/{driverId}/status` - Update driver status
- `GET /v1/drivers/{driverId}` - Get driver details

### Driver Offers

- `POST /v1/offers/{rideId}/send` - Send offer to driver
- `POST /v1/offers/{rideId}/accept` - Accept offer
- `POST /v1/offers/{rideId}/decline` - Decline offer

## API Usage Examples

### Create a Ride Request

```bash
curl -X POST http://localhost:8080/v1/rides \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant1" \
  -H "X-Region: us-east" \
  -d '{
    "riderId": 1,
    "pickupLatitude": 40.7128,
    "pickupLongitude": -74.0060,
    "pickupAddress": "New York, NY",
    "dropoffLatitude": 40.7589,
    "dropoffLongitude": -73.9851,
    "dropoffAddress": "Times Square, NY",
    "tier": "ECONOMY",
    "paymentMethod": "CARD"
  }'
```

### Get Ride Details

```bash
curl http://localhost:8080/v1/rides/{rideId}
```

### Start a Ride

```bash
curl -X POST "http://localhost:8080/v1/rides/{rideId}/start?driverId=1"
```

## Testing

### Run Unit Tests

```bash
./mvnw test
```

### Run Integration Tests

```bash
./mvnw test -Dtest=*IntegrationTest
```

## Project Structure

```
ride-hail-app/
├── src/
│   ├── main/
│   │   ├── java/com/example/ride_hail_app/
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── service/         # Business logic
│   │   │   ├── model/           # Domain models
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── repository/      # Data access layer
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── security/        # Security configuration
│   │   │   ├── exception/       # Exception handling
│   │   │   ├── statemachine/    # State machine for ride lifecycle
│   │   │   ├── indexing/        # Spatial indexing
│   │   │   ├── monitoring/      # New Relic integration
│   │   │   └── interceptor/     # Request interceptors
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/          # Frontend HTML
│   └── test/                    # Test classes
├── HLD.md                       # High-Level Design document
├── LLD.md                       # Low-Level Design document
├── PERFORMANCE_REPORT.md        # Performance report guide
├── NEW_RELIC_SETUP.md          # New Relic setup instructions
└── README.md                    # This file
```

## Key Design Decisions

### Scalability

- **Stateless APIs**: All services are stateless for horizontal scaling
- **Caching**: Redis for frequently accessed data
- **Async Processing**: Location updates and notifications processed asynchronously
- **Database Optimization**: Indexes, connection pooling, batch processing

### Performance

- **Spatial Indexing**: Geohash-based index for O(1) driver lookups
- **Batch Processing**: Location updates batched for high throughput
- **Connection Pooling**: HikariCP for efficient database connections
- **Query Optimization**: Indexed queries, batch inserts

### Reliability

- **Idempotency**: All write operations support idempotency keys
- **State Machine**: Validated state transitions prevent invalid operations
- **Distributed Locks**: Atomic driver allocation
- **Error Handling**: Comprehensive error handling with retry logic

## Monitoring

### New Relic Integration

See [NEW_RELIC_SETUP.md](NEW_RELIC_SETUP.md) for detailed setup instructions.

### Metrics Tracked

- API latency (p50, p95, p99)
- Request throughput
- Error rate
- Database query performance
- Business metrics (ride requests, matches, etc.)

## Documentation

- **HLD.md**: High-Level Design document
- **LLD.md**: Low-Level Design document
- **PERFORMANCE_REPORT.md**: Performance monitoring guide
- **NEW_RELIC_SETUP.md**: New Relic configuration guide

## Performance Targets

- **API Latency**: p95 < 1s
- **Driver Matching**: p95 < 1s
- **Throughput**: 10k ride requests/min (167 req/sec)
- **Location Updates**: 200k updates/sec
- **Error Rate**: < 1%

## Security

- **Input Validation**: All inputs validated
- **Rate Limiting**: Redis-based rate limiting
- **CORS**: Configured for cross-origin requests
- **Tenant Isolation**: Row-level data isolation

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For issues and questions, please open an issue in the repository.

---

**Version**: 1.0  
**Last Updated**: 2024

