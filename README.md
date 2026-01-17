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

## Prerequisites

- **Java 21** (OpenJDK 21 or Oracle JDK 21)
- **Maven** (included via Maven Wrapper - `./mvnw`)

## Setup

### 1. Install Java 21 (if not already installed)

**macOS (using Homebrew):**

```bash
brew install openjdk@21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH=$JAVA_HOME/bin:$PATH
```

**Linux:**

```bash
sudo apt-get install openjdk-21-jdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

**Windows:**

- Download OpenJDK 21 from [Adoptium](https://adoptium.net/)
- Set `JAVA_HOME` environment variable to JDK installation path
- Add `%JAVA_HOME%\bin` to PATH

### 2. Verify Java Installation

```bash
java -version
# Should show: openjdk version "21" or similar
```

## Starting the Application

### Option 1: Using Maven Wrapper (Recommended)

```bash
cd ride-hail-app
export JAVA_HOME=/opt/homebrew/opt/openjdk@21  # macOS only
export PATH=$JAVA_HOME/bin:$PATH
./mvnw spring-boot:run
```

### Option 2: Using Maven (if installed globally)

```bash
cd ride-hail-app
mvn spring-boot:run
```

### Option 3: Build and Run JAR

```bash
cd ride-hail-app
./mvnw clean package
java -jar target/ride-hail-app-0.0.1-SNAPSHOT.jar
```

## Accessing the Application

Once started, the application will be available at:

- **Frontend UI:** http://localhost:8080/
- **API Base URL:** http://localhost:8080/v1
- **Health Check:** http://localhost:8080/actuator/health

## Hot Reloading (Development)

The app includes Spring Boot DevTools for automatic restarts on code changes:

1. Start the app using `./mvnw spring-boot:run`
2. Make changes to Java files
3. The app will automatically restart when files are saved

**Note:** For frontend changes (`index.html`), you may need to refresh the browser.

## Stopping the Application

Press `Ctrl+C` in the terminal where the app is running, or:

```bash
pkill -f "spring-boot:run"
```

## Quick Test

1. Open browser: http://localhost:8080/
2. Click "Create Ride Request" with default values
3. Click "Get Ride" to see the created ride
4. Follow the sequence: Mark Driver Arriving → Start Ride → Complete Ride

## Default Test Data

The application automatically seeds test data on startup:

- **Rider ID:** 1
- **Driver ID:** 1
- **Tenant:** tenant1
- **Region:** us-east

## API Endpoints

### Ride Management

- `POST /v1/rides` - Create a ride request
- `GET /v1/rides/{rideId}` - Get ride details
- `POST /v1/rides/{rideId}/arriving` - Mark driver arriving
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

## Troubleshooting

### Port 8080 Already in Use

```bash
# Find and kill process using port 8080
lsof -ti:8080 | xargs kill -9
```

### Java Not Found

- Verify `JAVA_HOME` is set correctly
- Verify Java 21 is installed: `java -version`
- On macOS, ensure you've exported JAVA_HOME after installing

### Maven Wrapper Permission Denied

```bash
chmod +x ./mvnw
```

## Optional Services

The app can run without these, but they enhance functionality:

- **Redis:** For caching, spatial indexing, distributed locking
- **Kafka:** For asynchronous notifications
- **New Relic:** For application monitoring

See `application.properties` to configure these services.

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 21
- **Database**: PostgreSQL/MySQL (H2 for development)
- **Cache**: Redis (optional)
- **Messaging**: Apache Kafka (optional)
- **Monitoring**: New Relic APM (optional)
- **Build Tool**: Maven

## Documentation

- **HLD.md**: High-Level Design document
- **LLD.md**: Low-Level Design document
- **PERFORMANCE_REPORT.md**: Performance monitoring guide
- **NEW_RELIC_SETUP.md**: New Relic configuration guide
- **DELIVERABLES.md**: Complete feature list

## Performance Targets

- **API Latency**: p95 < 1s
- **Driver Matching**: p95 < 1s
- **Throughput**: 10k ride requests/min (167 req/sec)
- **Location Updates**: 200k updates/sec
- **Error Rate**: < 1%

## License

This project is licensed under the MIT License.

---

**Version**: 1.0  
**Last Updated**: 2024
