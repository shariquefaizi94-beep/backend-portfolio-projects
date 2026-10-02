# Architecture Documentation

## Overview

The Real-Time Streaming Analytics Platform is a distributed system designed for high-throughput event processing and real-time analytics. It follows an event-driven microservices architecture with clear separation of concerns.

## System Architecture

```
                                    ┌─────────────────────────────────────────────────────────────┐
                                    │                     Observability Layer                      │
                                    │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
                                    │  │ Prometheus  │  │   Grafana   │  │  Structured Logs    │  │
                                    │  │  (Metrics)  │  │ (Dashboards)│  │  (JSON/Logback)     │  │
                                    │  └──────▲──────┘  └─────────────┘  └─────────────────────┘  │
                                    │         │ /actuator/prometheus                               │
                                    └─────────┼───────────────────────────────────────────────────┘
                                              │
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                        Application Layer                                          │
│                                                                                                   │
│  ┌─────────────────────┐        ┌─────────────────────┐        ┌─────────────────────┐          │
│  │   Event Producer    │        │   Stream Processor  │        │    Analytics API    │          │
│  │                     │        │                     │        │                     │          │
│  │  • Event Generator  │        │  • Kafka Consumer   │        │  • REST Endpoints   │          │
│  │  • Kafka Producer   │        │  • Deduplication    │        │  • ES Queries       │          │
│  │  • REST Control     │        │  • Windowed Agg     │        │  • Redis Cache      │          │
│  │                     │        │  • ES Indexer       │        │  • OpenAPI/Swagger  │          │
│  │  Port: 8081         │        │  Port: 8082         │        │  Port: 8080         │          │
│  └──────────┬──────────┘        └──────────┬──────────┘        └──────────┬──────────┘          │
│             │                              │                              │                      │
└─────────────┼──────────────────────────────┼──────────────────────────────┼──────────────────────┘
              │                              │                              │
              ▼                              │                              │
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                      Infrastructure Layer                                         │
│                                                                                                   │
│  ┌─────────────────────┐        ┌─────────────────────┐        ┌─────────────────────┐          │
│  │   Apache Kafka      │◀───────│       Redis         │        │   Elasticsearch     │          │
│  │   (KRaft Mode)      │        │   (Dedup + Cache)   │◀───────│   (Metrics Store)   │          │
│  │                     │        │                     │        │                     │          │
│  │  • click-events     │        │  • dedup:* keys     │        │  • click-events     │          │
│  │  • 6 partitions     │        │  • analytics:* keys │        │  • aggregated-      │          │
│  │  • User-key routing │        │  • TTL-based expiry │        │    metrics          │          │
│  │                     │        │                     │        │                     │          │
│  │  Port: 9092         │        │  Port: 6379         │        │  Port: 9200         │          │
│  └─────────────────────┘        └─────────────────────┘        └─────────────────────┘          │
│                                                                                                   │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. Event Producer (`event-producer`)

**Responsibility**: Generate synthetic clickstream events at configurable throughput.

**Key Classes**:
- `EventGenerator`: Scheduled task that generates batches of synthetic events
- `EventProducer`: Publishes events to Kafka with userId as partition key
- `ClickEvent`: Immutable record representing a user interaction
- `GeneratorController`: REST API to start/stop/status generation

**Design Patterns**:
- Producer pattern for Kafka publishing
- Scheduled execution for continuous generation
- Metrics instrumentation for observability

**Configuration**:
```yaml
generator:
  events-per-batch: 100     # Batch size
  interval-ms: 100          # Scheduling interval
  user-pool-size: 1000      # Simulated users
```

### 2. Stream Processor (`stream-processor`)

**Responsibility**: Consume events, deduplicate, aggregate in time windows, and index to Elasticsearch.

**Key Classes**:
- `EventConsumer`: Batch Kafka consumer with manual acknowledgment
- `WindowedAggregator`: Tumbling window aggregation logic
- `ElasticsearchIndexer`: Async indexing to ES
- `AggregatedMetrics`: Windowed aggregation result

**Design Patterns**:
- Consumer pattern with batch processing
- Tumbling window aggregation
- Deduplication using Redis SET NX with TTL
- Async HTTP for ES indexing

**Data Flow**:
```
Kafka → Consumer → Dedup Check → Aggregator → ES Indexer
                       ↓
                   Redis SET NX
```

**Configuration**:
```yaml
aggregation:
  window-size-seconds: 60
  flush-interval-ms: 60000

deduplication:
  ttl-seconds: 3600
```

### 3. Analytics API (`analytics-api`)

**Responsibility**: Provide REST endpoints for querying aggregated analytics.

**Key Classes**:
- `AnalyticsController`: REST endpoints with OpenAPI annotations
- `AnalyticsService`: Query logic with Redis caching
- `DashboardSummary`: Aggregated view across multiple windows

**Design Patterns**:
- Cache-aside pattern with Redis
- HTTP client for ES queries
- DTO pattern for API responses

**Endpoints**:
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/analytics/dashboard` | Aggregated dashboard summary |
| GET | `/api/v1/analytics/metrics` | Recent windowed metrics |

## Data Models

### ClickEvent (Raw Event)

```java
record ClickEvent(
    String eventId,       // UUID, unique per event
    String userId,        // Partition key for ordering
    String sessionId,     // Session grouping
    String pageUrl,       // Page visited
    String referrer,      // Traffic source
    String eventType,     // click, view, purchase, etc.
    String deviceType,    // desktop, mobile, tablet
    String country,       // Geo location
    String city,
    Instant timestamp,    // Event time
    long processingTime   // For latency calculation
)
```

### AggregatedMetrics (Windowed Output)

```java
record AggregatedMetrics(
    String windowId,               // Time-based window identifier
    Instant windowStart,
    Instant windowEnd,
    long totalEvents,              // Count in window
    long uniqueUsers,              // Distinct users
    long uniqueSessions,           // Distinct sessions
    Map<String, Long> eventsByType,    // Breakdown by type
    Map<String, Long> eventsByCountry, // Breakdown by country
    Map<String, Long> eventsByDevice,  // Breakdown by device
    Map<String, Long> topPages,        // Top 10 pages
    double avgLatencyMs,           // Avg processing latency
    long maxLatencyMs,             // Max processing latency
    Instant computedAt             // Aggregation timestamp
)
```

## Processing Guarantees

### At-Least-Once Delivery

The system implements at-least-once delivery with idempotent processing:

1. **Kafka Consumer**: Manual acknowledgment after processing
2. **Deduplication**: Redis SETNX ensures events are processed once
3. **ES Indexing**: Document ID is event/window ID (idempotent PUT)

### Ordering Guarantees

- Events for the same user are ordered (same Kafka partition)
- Global ordering is not guaranteed (multiple partitions)
- Window aggregations are eventually consistent

## Scalability

### Horizontal Scaling

| Component | Scaling Strategy |
|-----------|-----------------|
| Event Producer | Add instances (stateless) |
| Stream Processor | Add instances (Kafka consumer group) |
| Analytics API | Add instances behind load balancer |
| Kafka | Add brokers, increase partitions |
| Redis | Redis Cluster for sharding |
| Elasticsearch | Add nodes to cluster |

### Throughput Estimates

With default configuration:
- Producer: ~10K events/second per instance
- Consumer: ~50K events/second per instance (batch processing)
- Aggregation: 60-second windows, ~6M events/window

## Technology Choices

### Why Kafka KRaft?

- Eliminates Zookeeper operational complexity
- Faster broker startup and recovery
- Simplified deployment topology
- Native Kafka metadata management

### Why In-Memory Aggregation?

This implementation uses simple in-memory aggregation for clarity. Production alternatives:

| Technology | Use Case |
|------------|----------|
| Kafka Streams | Stateful processing with RocksDB |
| Apache Flink | Complex event processing, exactly-once |
| ksqlDB | SQL-based stream processing |

### Why Elasticsearch for Metrics?

- Flexible schema for varied aggregations
- Built-in time-series optimizations
- Powerful aggregation queries
- Kibana for ad-hoc exploration

## Failure Handling

### Component Failures

| Failure | Impact | Recovery |
|---------|--------|----------|
| Kafka broker | Temporary message delay | Automatic failover |
| Redis down | No dedup, potential duplicates | ES idempotent writes |
| ES down | No metrics storage | Retry with backoff |
| Stream Processor crash | Consumer rebalance | Resume from offset |

### Data Loss Prevention

- Kafka replication factor > 1 in production
- Redis AOF persistence for dedup state
- ES snapshots for metrics history

## Security Considerations

Production deployments should add:

1. **Kafka**: SASL/SCRAM authentication, TLS encryption
2. **Redis**: AUTH password, TLS
3. **Elasticsearch**: API keys, TLS, RBAC
4. **APIs**: OAuth2/JWT authentication, rate limiting
5. **Network**: Service mesh (Istio), network policies

## Monitoring Strategy

### Metrics (Prometheus)

- Business: events generated, processed, duplicates
- Performance: latency percentiles, throughput
- Resources: JVM memory, GC, threads

### Logs (Structured JSON)

- Correlation IDs for tracing
- Log levels: ERROR for failures, INFO for checkpoints
- Sensitive data masking

### Dashboards (Grafana)

- Real-time throughput graphs
- Error rate alerts
- Resource utilization
