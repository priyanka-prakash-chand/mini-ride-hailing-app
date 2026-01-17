# Performance Report Guide
## New Relic Monitoring & Analysis

### 1. Overview

This document provides guidance on generating and interpreting performance reports using New Relic APM for the ride-hailing application.

### 2. New Relic Setup

#### 2.1 Prerequisites

1. **New Relic Account**: Sign up at https://newrelic.com (100GB free tier available)
2. **API Key**: Generate API key from Account Settings → API Keys
3. **Account ID**: Found in Account Settings

#### 2.2 Configuration

Update `application.properties`:

```properties
management.newrelic.metrics.export.enabled=true
management.newrelic.metrics.export.api-key=${NEW_RELIC_API_KEY}
management.newrelic.metrics.export.account-id=${NEW_RELIC_ACCOUNT_ID}
management.newrelic.metrics.export.app-name=RideHailApp
management.newrelic.metrics.export.step=60s
```

Set environment variables:
```bash
export NEW_RELIC_API_KEY=your-api-key-here
export NEW_RELIC_ACCOUNT_ID=your-account-id-here
```

### 3. Key Metrics to Monitor

#### 3.1 Application Performance Metrics

**API Latency**:
- **Target**: p95 < 1s for ride creation
- **Location**: APM → Application → Response Time
- **Screenshot**: Capture p50, p95, p99 latencies

**Request Rate**:
- **Target**: Handle 10k requests/min (167 req/sec)
- **Location**: APM → Application → Throughput
- **Screenshot**: Show requests per second graph

**Error Rate**:
- **Target**: < 1% error rate
- **Location**: APM → Application → Error Rate
- **Screenshot**: Show error percentage over time

#### 3.2 Database Performance Metrics

**Query Latency**:
- **Target**: p95 < 500ms
- **Location**: APM → Database → Query Time
- **Screenshot**: Show slowest queries

**Database Throughput**:
- **Location**: APM → Database → Throughput
- **Screenshot**: Show queries per second

**Connection Pool**:
- **Location**: APM → Database → Connection Pool
- **Screenshot**: Show active/idle connections

#### 3.3 Business Metrics

**Ride Requests**:
- **Location**: Custom Metrics → `ride.requests.count`
- **Screenshot**: Show ride requests per minute

**Driver Matches**:
- **Location**: Custom Metrics → `ride.matches.count`
- **Screenshot**: Show matches per minute

**Matching Latency**:
- **Location**: Custom Metrics → `driver.matching.latency`
- **Screenshot**: Show matching time distribution

**Surge Pricing**:
- **Location**: Custom Metrics → `surge.multiplier`
- **Screenshot**: Show surge multiplier over time

### 4. Creating Performance Reports

#### 4.1 Dashboard Setup

1. **Navigate to**: New Relic → Dashboards → Create Dashboard
2. **Add Widgets**:
   - API Response Time (Line Chart)
   - Request Throughput (Line Chart)
   - Error Rate (Line Chart)
   - Database Query Time (Bar Chart)
   - Custom Business Metrics (Line Charts)
   - Cache Hit Rate (Gauge)

3. **Screenshot**: Capture full dashboard view

#### 4.2 Performance Test Scenarios

**Scenario 1: Normal Load**
- **Load**: 100 requests/sec
- **Duration**: 10 minutes
- **Metrics to Capture**:
  - API latency (p50, p95, p99)
  - Request throughput
  - Error rate
  - Database query latency

**Scenario 2: Peak Load**
- **Load**: 200 requests/sec (simulating 10k/min)
- **Duration**: 5 minutes
- **Metrics to Capture**:
  - API latency degradation
  - System resource usage
  - Cache hit rate
  - Database connection pool usage

**Scenario 3: Location Update Load**
- **Load**: 200k location updates/sec
- **Duration**: 5 minutes
- **Metrics to Capture**:
  - Location update processing latency
  - Batch processing efficiency
  - Database write throughput

**Scenario 4: Driver Matching Performance**
- **Load**: 100 ride requests/sec
- **Duration**: 5 minutes
- **Metrics to Capture**:
  - Matching latency (p95 < 1s)
  - Matching success rate
  - Spatial index lookup time

### 5. Screenshot Checklist

#### 5.1 Application Performance
- [ ] API Response Time (p50, p95, p99)
- [ ] Request Throughput (requests/sec)
- [ ] Error Rate (%)
- [ ] Top Transactions (slowest endpoints)

#### 5.2 Database Performance
- [ ] Database Query Time (slowest queries)
- [ ] Database Throughput (queries/sec)
- [ ] Connection Pool Status
- [ ] Query Breakdown by Operation

#### 5.3 Business Metrics
- [ ] Ride Requests per Minute
- [ ] Driver Matches per Minute
- [ ] Matching Latency Distribution
- [ ] Surge Pricing Multiplier
- [ ] Ride Completion Rate

#### 5.4 System Resources
- [ ] CPU Usage
- [ ] Memory Usage
- [ ] JVM Heap Usage
- [ ] Thread Pool Status

#### 5.5 Custom Dashboards
- [ ] Main Performance Dashboard
- [ ] Business Metrics Dashboard
- [ ] Error Analysis Dashboard

### 6. Sample Performance Report Structure

#### 6.1 Executive Summary

```
Performance Test Results - Ride Hailing Application
Date: [Date]
Duration: [Duration]
Load: [Load Description]

Key Findings:
- API p95 latency: [X]ms (Target: <1000ms) ✓
- Request throughput: [X] req/sec (Target: 167 req/sec) ✓
- Error rate: [X]% (Target: <1%) ✓
- Matching latency p95: [X]ms (Target: <1000ms) ✓
```

#### 6.2 Detailed Metrics

**API Performance**:
- Average Response Time: [X]ms
- p50 Response Time: [X]ms
- p95 Response Time: [X]ms
- p99 Response Time: [X]ms
- Requests per Second: [X]
- Error Rate: [X]%

**Database Performance**:
- Average Query Time: [X]ms
- Slowest Query: [Query] - [X]ms
- Queries per Second: [X]
- Connection Pool Utilization: [X]%

**Business Metrics**:
- Ride Requests: [X] per minute
- Driver Matches: [X] per minute
- Matching Success Rate: [X]%
- Average Matching Latency: [X]ms

#### 6.3 Screenshots

Include screenshots of:
1. New Relic Dashboard (full view)
2. API Response Time Chart
3. Database Query Performance
4. Custom Business Metrics
5. Error Analysis

### 7. Interpreting Results

#### 7.1 Performance Indicators

**Good Performance**:
- p95 API latency < 1s
- Error rate < 1%
- Database query p95 < 500ms
- Cache hit rate > 80%
- Matching latency p95 < 1s

**Performance Issues**:
- p95 API latency > 1s → Check database queries, cache efficiency
- Error rate > 1% → Check error logs, exception handling
- Database query p95 > 500ms → Optimize queries, add indexes
- Cache hit rate < 80% → Review caching strategy, TTLs
- Matching latency p95 > 1s → Optimize spatial index, driver lookup

#### 7.2 Bottleneck Identification

**Database Bottlenecks**:
- High query latency → Add indexes, optimize queries
- Connection pool exhaustion → Increase pool size
- Slow writes → Enable batch processing

**Application Bottlenecks**:
- High CPU usage → Optimize algorithms, add caching
- High memory usage → Review object creation, memory leaks
- Thread pool exhaustion → Increase thread pool size

**Cache Bottlenecks**:
- Low hit rate → Review TTLs, cache keys
- High cache latency → Check Redis performance

### 8. Alert Configuration

#### 8.1 Critical Alerts

1. **API Latency Alert**:
   - Condition: p95 response time > 1s
   - Duration: 5 minutes
   - Notification: Email, Slack

2. **Error Rate Alert**:
   - Condition: Error rate > 1%
   - Duration: 5 minutes
   - Notification: Email, Slack

3. **Database Query Alert**:
   - Condition: p95 query time > 500ms
   - Duration: 5 minutes
   - Notification: Email

4. **Matching Failure Alert**:
   - Condition: Matching failure rate > 5%
   - Duration: 5 minutes
   - Notification: Email, Slack

#### 8.2 Warning Alerts

1. **High Request Rate**:
   - Condition: Request rate > 200 req/sec
   - Duration: 10 minutes
   - Notification: Email

2. **Low Cache Hit Rate**:
   - Condition: Cache hit rate < 70%
   - Duration: 10 minutes
   - Notification: Email

### 9. Generating Reports

#### 9.1 Automated Reports

1. **Navigate to**: New Relic → Reports → Create Report
2. **Select Metrics**: Choose relevant metrics
3. **Schedule**: Daily/Weekly/Monthly
4. **Recipients**: Add email addresses

#### 9.2 Manual Reports

1. **Navigate to**: New Relic → Dashboards
2. **Select Time Range**: Choose date range
3. **Export**: Click Export → PDF/PNG
4. **Save**: Download and save for documentation

### 10. Sample Load Test Script

```bash
#!/bin/bash
# Load test script using Apache Bench (ab)

# Test ride creation endpoint
ab -n 10000 -c 100 -p ride_request.json -T application/json \
   -H "X-Tenant-Id: tenant1" \
   -H "X-Region: us-east" \
   http://localhost:8080/v1/rides

# Test ride retrieval endpoint
ab -n 10000 -c 100 http://localhost:8080/v1/rides/{rideId}
```

### 11. Performance Optimization Recommendations

Based on New Relic metrics, optimize:

1. **Slow Database Queries**: Add indexes, optimize joins
2. **High API Latency**: Add caching, optimize algorithms
3. **Low Cache Hit Rate**: Review TTLs, cache keys
4. **High Error Rate**: Review error logs, fix bugs
5. **Resource Exhaustion**: Scale horizontally, optimize resource usage

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Author**: Development Team

