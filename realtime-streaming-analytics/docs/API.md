# API Documentation

## Overview

The Real-Time Streaming Analytics Platform exposes REST APIs across three services:
- **Analytics API** (port 8080): Query aggregated analytics
- **Event Producer** (port 8081): Control event generation
- **Stream Processor** (port 8082): Monitoring only (actuator)

Interactive API documentation is available via Swagger UI at:
- http://localhost:8080/swagger-ui.html

## Analytics API

Base URL: `http://localhost:8080/api/v1`

### Get Dashboard Summary

Retrieves an aggregated summary across multiple time windows.

**Request**
```http
GET /api/v1/analytics/dashboard?windows=10
```

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| windows | int | 10 | Number of recent windows to aggregate |

**Response**
```json
{
  "totalEvents": 156000,
  "uniqueUsers": 1245,
  "uniqueSessions": 3567,
  "avgLatencyMs": 12.45,
  "eventsByType": {
    "page_view": 46800,
    "click": 39000,
    "scroll": 31200,
    "form_submit": 23400,
    "purchase": 15600
  },
  "eventsByCountry": {
    "US": 62400,
    "UK": 31200,
    "DE": 23400,
    "FR": 15600,
    "JP": 12480,
    "IN": 10920
  },
  "eventsByDevice": {
    "desktop": 93600,
    "mobile": 46800,
    "tablet": 15600
  },
  "windowsAggregated": 10,
  "computedAt": "2024-01-15T14:30:00Z"
}
```

**Response Fields**

| Field | Type | Description |
|-------|------|-------------|
| totalEvents | long | Sum of events across all windows |
| uniqueUsers | long | Approximate unique user count |
| uniqueSessions | long | Approximate unique session count |
| avgLatencyMs | double | Average processing latency |
| eventsByType | Map | Event count by interaction type |
| eventsByCountry | Map | Event count by country |
| eventsByDevice | Map | Event count by device type |
| windowsAggregated | int | Number of windows in calculation |
| computedAt | ISO-8601 | Response timestamp |

**cURL Example**
```bash
curl -X GET "http://localhost:8080/api/v1/analytics/dashboard?windows=10" \
  -H "Accept: application/json" | jq .
```

---

### Get Recent Metrics

Retrieves individual windowed metric records from Elasticsearch.

**Request**
```http
GET /api/v1/analytics/metrics?count=20
```

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| count | int | 20 | Number of recent windows to retrieve |

**Response**
```json
[
  {
    "windowId": "2024-01-15T14-29-00Z",
    "windowStart": "2024-01-15T14:29:00Z",
    "windowEnd": "2024-01-15T14:30:00Z",
    "totalEvents": 15600,
    "uniqueUsers": 487,
    "uniqueSessions": 892,
    "eventsByType": {
      "page_view": 4680,
      "click": 3900,
      "scroll": 3120,
      "form_submit": 2340,
      "purchase": 1560
    },
    "eventsByCountry": {
      "US": 6240,
      "UK": 3120,
      "DE": 2340,
      "FR": 1560,
      "JP": 1248,
      "IN": 1092
    },
    "eventsByDevice": {
      "desktop": 9360,
      "mobile": 4680,
      "tablet": 1560
    },
    "topPages": {
      "/home": 3900,
      "/products": 3120,
      "/cart": 2340,
      "/checkout": 1950,
      "/search": 1560
    },
    "avgLatencyMs": 11.23,
    "maxLatencyMs": 89,
    "computedAt": "2024-01-15T14:30:01Z"
  }
]
```

**cURL Example**
```bash
curl -X GET "http://localhost:8080/api/v1/analytics/metrics?count=5" \
  -H "Accept: application/json" | jq .
```

---

## Event Producer API

Base URL: `http://localhost:8081/api/v1`

### Start Event Generation

Starts the synthetic event generator.

**Request**
```http
POST /api/v1/generator/start
```

**Response**
```json
{
  "status": "started"
}
```

**cURL Example**
```bash
curl -X POST "http://localhost:8081/api/v1/generator/start" | jq .
```

---

### Stop Event Generation

Stops the synthetic event generator.

**Request**
```http
POST /api/v1/generator/stop
```

**Response**
```json
{
  "status": "stopped",
  "totalGenerated": 1567890
}
```

**cURL Example**
```bash
curl -X POST "http://localhost:8081/api/v1/generator/stop" | jq .
```

---

### Get Generator Status

Returns current generator state.

**Request**
```http
GET /api/v1/generator/status
```

**Response**
```json
{
  "running": true,
  "totalGenerated": 1234567
}
```

**cURL Example**
```bash
curl -X GET "http://localhost:8081/api/v1/generator/status" | jq .
```

---

## Actuator Endpoints

All services expose Spring Boot Actuator endpoints for monitoring.

### Health Check

```http
GET /actuator/health
```

**Response**
```json
{
  "status": "UP",
  "components": {
    "kafka": { "status": "UP" },
    "redis": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### Prometheus Metrics

```http
GET /actuator/prometheus
```

Returns metrics in Prometheus exposition format:
```
# HELP events_generated_total Total synthetic events generated
# TYPE events_generated_total counter
events_generated_total 1234567.0

# HELP kafka_events_sent_total Events sent to Kafka
# TYPE kafka_events_sent_total counter
kafka_events_sent_total 1234500.0
```

### Info

```http
GET /actuator/info
```

Returns application info including version, build time.

---

## Error Responses

### Standard Error Format

```json
{
  "timestamp": "2024-01-15T14:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to query Elasticsearch",
  "path": "/api/v1/analytics/dashboard"
}
```

### HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 400 | Bad Request - Invalid parameters |
| 404 | Not Found - Resource doesn't exist |
| 500 | Internal Error - Server-side failure |
| 503 | Service Unavailable - Dependency down |

---

## Rate Limiting

> **Note**: Rate limiting is not implemented in this demo. Production deployments should add rate limiting using API Gateway or Spring Cloud Gateway.

Recommended limits:
- Dashboard: 100 requests/minute per client
- Metrics: 100 requests/minute per client
- Generator control: 10 requests/minute per client

---

## Caching

### Analytics API Caching

Dashboard summaries are cached in Redis:
- Cache key: `analytics:dashboard:{windowCount}`
- TTL: 30 seconds (configurable)
- Cache invalidation: TTL-based expiry

To bypass cache, no direct mechanism exists. Wait for TTL expiry or adjust `cache.ttl-seconds` configuration.

---

## Authentication

> **Note**: Authentication is not implemented in this demo. Production deployments should add:

- OAuth2/JWT for API authentication
- API keys for service-to-service calls
- mTLS for internal service mesh

Example with JWT (future implementation):
```bash
curl -X GET "http://localhost:8080/api/v1/analytics/dashboard" \
  -H "Authorization: Bearer <token>" \
  -H "Accept: application/json"
```
