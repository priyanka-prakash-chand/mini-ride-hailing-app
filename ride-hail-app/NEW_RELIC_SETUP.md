# New Relic Monitoring Setup Guide

## Overview
This application is integrated with New Relic for comprehensive monitoring of API performance, database queries, and business metrics.

## Setup Instructions

### 1. Create New Relic Account
1. Sign up at https://newrelic.com/signup (100GB free for new accounts)
2. Create a new account or log in to existing account

### 2. Get API Key and Account ID
1. Go to https://one.newrelic.com/admin-portal/api-keys/home
2. Create a new API key (or use existing)
3. Copy your Account ID from the dashboard

### 3. Configure Environment Variables
Set the following environment variables:

```bash
export NEW_RELIC_API_KEY="NRAK-CMAGEFZ9SZB34M2C718IVDWW288"
export NEW_RELIC_ACCOUNT_ID="7599680"
export NEW_RELIC_APP_NAME="ride-hail-app"
export NEW_RELIC_ENVIRONMENT="production"  # or "development", "staging"
```

Or update `application.properties` directly:
```properties
management.newrelic.metrics.export.api-key=your-api-key-here
management.newrelic.metrics.export.account-id=your-account-id-here
newrelic.config.app_name=ride-hail-app
newrelic.config.environment=production
```

### 4. Verify Configuration
After starting the application, check New Relic dashboard:
- Go to https://one.newrelic.com
- Navigate to "APM & Services" → "ride-hail-app"
- You should see metrics appearing within 1-2 minutes

## Metrics Tracked

### API Performance Metrics
- **api.latency**: API endpoint latency (p50, p95, p99)
  - Tags: endpoint, method, region, tenant
  - Alerts: p95 > 1s, p99 > 2s

### Database Metrics
- **database.query.latency**: Database query performance
  - Tags: query name
  - Alerts: query > 500ms

### Business Metrics
- **ride.requests.total**: Total ride requests
- **ride.matches.total**: Successful driver matches
- **ride.completions.total**: Completed rides
- **ride.cancellations.total**: Cancelled rides
- **location.updates.total**: Location updates processed
- **driver.matches.total**: Driver matching events

### Driver Matching Metrics
- **driver.matching.latency**: Driver matching performance
  - Tags: region, success
  - Alerts: latency > 1s (p95)

### Error Metrics
- **api.errors.total**: API errors by endpoint and type
- **database.errors.total**: Database errors by query
- **matching.failures.total**: Driver matching failures

### Gauge Metrics (Real-time)
- **drivers.active**: Number of active drivers per region
- **rides.pending**: Number of pending rides per region

## Setting Up Alerts in New Relic

### 1. API Latency Alert
1. Go to New Relic → Alerts & AI → Alert Conditions
2. Create new condition:
   - **Metric**: `api.latency`
   - **Threshold**: p95 > 1000ms (1 second)
   - **Duration**: 5 minutes
   - **Notification**: Email/Slack/PagerDuty

### 2. Database Query Alert
1. Create alert condition:
   - **Metric**: `database.query.latency`
   - **Threshold**: Average > 500ms
   - **Duration**: 5 minutes
   - **Notification**: Email/Slack

### 3. Driver Matching Alert
1. Create alert condition:
   - **Metric**: `driver.matching.latency`
   - **Threshold**: p95 > 1000ms
   - **Duration**: 5 minutes
   - **Notification**: Email/Slack

### 4. High Error Rate Alert
1. Create alert condition:
   - **Metric**: `api.errors.total`
   - **Threshold**: Rate > 100 errors/minute
   - **Duration**: 2 minutes
   - **Notification**: PagerDuty (critical)

### 5. Matching Failure Alert
1. Create alert condition:
   - **Metric**: `matching.failures.total`
   - **Threshold**: Rate > 50 failures/minute
   - **Duration**: 5 minutes
   - **Notification**: Email/Slack

## NRQL Queries for Analysis

### Find Slowest API Endpoints
```sql
SELECT average(duration) 
FROM Metric 
WHERE metricName = 'api.latency' 
FACET endpoint 
SINCE 1 hour ago 
ORDER BY average(duration) DESC
```

### Database Query Performance
```sql
SELECT average(duration), percentile(duration, 95), percentile(duration, 99)
FROM Metric 
WHERE metricName = 'database.query.latency' 
FACET query 
SINCE 1 hour ago
```

### Driver Matching Success Rate
```sql
SELECT count(*) 
FROM Metric 
WHERE metricName = 'driver.matching.latency' 
FACET success 
SINCE 1 hour ago
```

### Error Rate by Endpoint
```sql
SELECT rate(count(*), 1 minute) as 'errors_per_minute'
FROM Metric 
WHERE metricName = 'api.errors.total' 
FACET endpoint 
SINCE 1 hour ago
```

### Ride Request Volume by Region
```sql
SELECT rate(count(*), 1 minute) as 'requests_per_minute'
FROM Metric 
WHERE metricName = 'ride.requests.total' 
FACET region 
SINCE 1 hour ago
```

## Dashboard Setup

### Create Custom Dashboard
1. Go to New Relic → Dashboards → Create Dashboard
2. Add widgets for:
   - API Latency (Line Chart)
   - Database Query Performance (Table)
   - Business Metrics (Bar Chart)
   - Error Rate (Line Chart)
   - Active Drivers (Gauge)
   - Pending Rides (Gauge)

### Recommended Widgets
1. **API Performance**: `api.latency` with p50, p95, p99
2. **Database Performance**: `database.query.latency` by query
3. **Business Metrics**: `ride.*` metrics grouped by region
4. **Error Tracking**: `api.errors.total` by endpoint
5. **Matching Performance**: `driver.matching.latency` success rate

## Troubleshooting

### Metrics Not Appearing
1. Check API key and account ID are correct
2. Verify network connectivity to New Relic
3. Check application logs for New Relic errors
4. Ensure `management.newrelic.metrics.export.enabled=true`

### High Metric Volume
- New Relic free tier: 100GB/month
- Current setup sends metrics every 10 seconds
- Estimated usage: ~50-100MB/day (well within free tier)

### Performance Impact
- Metrics collection is async and non-blocking
- Minimal performance impact (<1% overhead)
- Can be disabled in development if needed

## Production Recommendations

1. **Enable APM Agent** (optional): Install New Relic Java agent for deeper insights
2. **Set Up Alert Policies**: Group related alerts for better management
3. **Create Runbooks**: Document response procedures for each alert
4. **Regular Review**: Weekly review of slow queries and bottlenecks
5. **Capacity Planning**: Monitor trends to predict scaling needs

## Additional Resources
- New Relic Documentation: https://docs.newrelic.com
- Micrometer New Relic: https://micrometer.io/docs/registry/new-relic
- NRQL Query Reference: https://docs.newrelic.com/docs/query-your-data/nrql-query-tutorials/

