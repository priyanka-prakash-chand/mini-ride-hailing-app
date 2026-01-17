# Quick Start Guide

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

## Next Steps

- Read `README.md` for detailed project information
- Check `HLD.md` for high-level design
- Review `LLD.md` for low-level design details
- See `DELIVERABLES.md` for complete feature list
