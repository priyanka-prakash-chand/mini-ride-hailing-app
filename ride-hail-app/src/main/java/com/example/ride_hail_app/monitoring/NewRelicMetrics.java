package com.example.ride_hail_app.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * Custom metrics for New Relic monitoring
 * Tracks API latencies, bottlenecks, and business metrics
 */
@Component
public class NewRelicMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // API latency timers
    private final Map<String, Timer> apiTimers = new ConcurrentHashMap<>();
    
    // Business metrics counters
    private final Counter rideRequestsCounter;
    private final Counter rideMatchesCounter;
    private final Counter rideCompletionsCounter;
    private final Counter rideCancellationsCounter;
    private final Counter locationUpdatesCounter;
    private final Counter driverMatchesCounter;
    
    // Error counters
    private final Counter apiErrorsCounter;
    private final Counter databaseErrorsCounter;
    private final Counter matchingFailuresCounter;
    
    public NewRelicMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize business metrics
        this.rideRequestsCounter = Counter.builder("ride.requests.total")
            .description("Total number of ride requests")
            .tag("application", "ride-hail-app")
            .register(meterRegistry);
        
        this.rideMatchesCounter = Counter.builder("ride.matches.total")
            .description("Total number of successful driver matches")
            .tag("application", "ride-hail-app")
            .register(meterRegistry);
        
        this.rideCompletionsCounter = Counter.builder("ride.completions.total")
            .description("Total number of completed rides")
            .tag("application", "ride-hail-app")
            .register(meterRegistry);
        
        this.rideCancellationsCounter = Counter.builder("ride.cancellations.total")
            .description("Total number of cancelled rides")
            .tag("application", "ride-hail-app")
            .register(meterRegistry);
        
        this.locationUpdatesCounter = Counter.builder("location.updates.total")
            .description("Total number of location updates")
            .tag("application", "ride-hail-app")
            .register(meterRegistry);
        
        this.driverMatchesCounter = Counter.builder("driver.matches.total")
            .description("Total number of driver matches")
            .tag("application", "ride-hail-app")
            .register(meterRegistry);
        
        // Error metrics
        this.apiErrorsCounter = Counter.builder("api.errors.total")
            .description("Total number of API errors")
            .tag("application", "ride-hail-app")
            .register(meterRegistry);
        
        this.databaseErrorsCounter = Counter.builder("database.errors.total")
            .description("Total number of database errors")
            .tag("application", "ride-hail-app")
            .register(meterRegistry);
        
        this.matchingFailuresCounter = Counter.builder("matching.failures.total")
            .description("Total number of driver matching failures")
            .tag("application", "ride-hail-app")
            .register(meterRegistry);
    }
    
    /**
     * Record API latency
     */
    public Timer.Sample startApiTimer(String endpoint, String method) {
        String timerName = "api.latency";
        Timer timer = apiTimers.computeIfAbsent(timerName, name ->
            Timer.builder(name)
                .description("API endpoint latency")
                .tag("endpoint", endpoint)
                .tag("method", method)
                .tag("application", "ride-hail-app")
                .publishPercentiles(0.5, 0.95, 0.99) // Track p50, p95, p99
                .publishPercentileHistogram(true)
                .register(meterRegistry)
        );
        return Timer.start(meterRegistry);
    }
    
    /**
     * Record API latency with custom tags
     */
    public void recordApiLatency(String endpoint, String method, long durationMs, String region, String tenantId) {
        Timer.builder("api.latency")
            .description("API endpoint latency")
            .tag("endpoint", endpoint)
            .tag("method", method)
            .tag("region", region != null ? region : "unknown")
            .tag("tenant", tenantId != null ? tenantId : "unknown")
            .tag("application", "ride-hail-app")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Record database query latency
     */
    public void recordDatabaseQuery(String queryName, long durationMs) {
        Timer.builder("database.query.latency")
            .description("Database query latency")
            .tag("query", queryName)
            .tag("application", "ride-hail-app")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Record driver matching latency
     */
    public void recordMatchingLatency(long durationMs, String region, boolean success) {
        Timer.builder("driver.matching.latency")
            .description("Driver matching latency")
            .tag("region", region != null ? region : "unknown")
            .tag("success", String.valueOf(success))
            .tag("application", "ride-hail-app")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Increment ride requests counter
     */
    public void incrementRideRequests(String region, String tier) {
        meterRegistry.counter("ride.requests.total", 
            "region", region != null ? region : "unknown",
            "tier", tier != null ? tier : "unknown",
            "application", "ride-hail-app"
        ).increment();
    }
    
    /**
     * Increment ride matches counter
     */
    public void incrementRideMatches(String region) {
        meterRegistry.counter("ride.matches.total",
            "region", region != null ? region : "unknown",
            "application", "ride-hail-app"
        ).increment();
    }
    
    /**
     * Increment ride completions counter
     */
    public void incrementRideCompletions(String region) {
        meterRegistry.counter("ride.completions.total",
            "region", region != null ? region : "unknown",
            "application", "ride-hail-app"
        ).increment();
    }
    
    /**
     * Increment ride cancellations counter
     */
    public void incrementRideCancellations(String region, String reason) {
        meterRegistry.counter("ride.cancellations.total",
            "region", region != null ? region : "unknown",
            "reason", reason != null ? reason : "unknown",
            "application", "ride-hail-app"
        ).increment();
    }
    
    /**
     * Increment location updates counter
     */
    public void incrementLocationUpdates() {
        locationUpdatesCounter.increment();
    }
    
    /**
     * Increment driver matches counter
     */
    public void incrementDriverMatches(String region) {
        meterRegistry.counter("driver.matches.total",
            "region", region != null ? region : "unknown",
            "application", "ride-hail-app"
        ).increment();
    }
    
    /**
     * Increment API errors counter
     */
    public void incrementApiErrors(String endpoint, String errorType) {
        meterRegistry.counter("api.errors.total",
            "endpoint", endpoint != null ? endpoint : "unknown",
            "error_type", errorType != null ? errorType : "unknown",
            "application", "ride-hail-app"
        ).increment();
    }
    
    /**
     * Increment database errors counter
     */
    public void incrementDatabaseErrors(String queryName) {
        meterRegistry.counter("database.errors.total",
            "query", queryName != null ? queryName : "unknown",
            "application", "ride-hail-app"
        ).increment();
    }
    
    /**
     * Increment matching failures counter
     */
    public void incrementMatchingFailures(String region, String reason) {
        meterRegistry.counter("matching.failures.total",
            "region", region != null ? region : "unknown",
            "reason", reason != null ? reason : "unknown",
            "application", "ride-hail-app"
        ).increment();
    }
    
    /**
     * Register gauge for active drivers
     */
    public void registerActiveDriversGauge(String region, java.util.function.Supplier<Number> supplier) {
        Gauge.builder("drivers.active", supplier)
            .description("Number of active drivers")
            .tag("region", region)
            .tag("application", "ride-hail-app")
            .register(meterRegistry);
    }
    
    /**
     * Register gauge for pending rides
     */
    public void registerPendingRidesGauge(String region, java.util.function.Supplier<Number> supplier) {
        Gauge.builder("rides.pending", supplier)
            .description("Number of pending rides")
            .tag("region", region)
            .tag("application", "ride-hail-app")
            .register(meterRegistry);
    }
}

