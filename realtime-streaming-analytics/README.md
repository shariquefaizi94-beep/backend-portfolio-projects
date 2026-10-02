# Real-Time Streaming Analytics Platform

[![Build](https://github.com/shariquefaizi94-beep/realtime-streaming-analytics/actions/workflows/build.yml/badge.svg)](https://github.com/shariquefaizi94-beep/realtime-streaming-analytics/actions/workflows/build.yml)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Kafka-KRaft-orange.svg)](https://kafka.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A high-throughput event streaming platform demonstrating Kafka ingestion, windowed aggregation, Elasticsearch indexing, and real-time analytics APIs. Designed for processing millions of events with sub-second latency.

## 🏗️ Architecture

```
┌─────────────────┐     ┌─────────────┐     ┌──────────────────┐
│  Event Producer │────▶│    Kafka    │────▶│ Stream Processor │
│  (Generator)    │     │  (KRaft)    │     │ (Consumer)       │
└─────────────────┘     └─────────────┘     └────────┬─────────┘
                                                     │
                        ┌────────────────────────────┼────────────────────────────┐
                        │                            │                            │
                        ▼                            ▼                            ▼
               ┌────────────────┐          ┌─────────────────┐          ┌─────────────────┐
               │     Redis      │          │  Elasticsearch  │          │   Prometheus    │
               │ (Deduplication)│          │   (Metrics)     │          │   (Monitoring)  │
               └────────────────┘          └────────┬────────┘          └─────────────────┘
                                                    │
                                                    ▼
                                           ┌─────────────────┐
                                           │  Analytics API  │
                                           │  (REST/OpenAPI) │
                                           └─────────────────┘
```

## ✨ Key Features

| Feature | Description |
|---------|-------------|
| **High-Throughput Ingestion** | Kafka-based event ingestion capable of 100K+ events/second |
| **Windowed Aggregation** | Tumbling window aggregations (configurable window size) |
| **Deduplication** | Redis-backed exactly-once processing with TTL-based dedup |
| **Real-Time Queries** | REST API for analytics with Redis caching |
| **Observability** | Prometheus metrics, Grafana dashboards, structured logging |
| **Scalable Design** | Stateless services, Kafka partitioning, horizontal scaling |

## 🛠️ Tech Stack

- **Runtime**: Java 21, Spring Boot 3.2.5
- **Messaging**: Apache Kafka 7.6.0 (KRaft mode - no Zookeeper)
- **Cache/Dedup**: Redis 7.2
- **Search/Storage**: Elasticsearch 8.13.0
- **Monitoring**: Prometheus 2.51.0, Grafana 10.4.0
- **API Docs**: OpenAPI 3.0 / Swagger UI
- **Containers**: Docker, Docker Compose

## 📁 Project Structure

```
realtime-streaming-analytics/
├── event-producer/          # Synthetic event generator
│   └── src/main/java/.../
│       ├── generator/       # Event generation logic
│       ├── kafka/           # Kafka producer
│       └── model/           # ClickEvent record
├── stream-processor/        # Kafka consumer & aggregation
│   └── src/main/java/.../
│       ├── aggregation/     # Windowed aggregator
│       ├── elasticsearch/   # ES indexer
│       └── kafka/           # Consumer with dedup
├── analytics-api/           # REST API for queries
│   └── src/main/java/.../
│       ├── controller/      # REST endpoints
│       └── service/         # Query service with caching
├── docker/                  # Docker Compose & configs
│   ├── prometheus/          # Prometheus config
│   └── grafana/             # Grafana dashboards
└── docs/                    # Architecture documentation
```

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose

### Build & Run

```bash
# Clone the repository
git clone https://github.com/shariquefaizi94-beep/realtime-streaming-analytics.git
cd realtime-streaming-analytics

# Build all modules
make build

# Start infrastructure (Kafka, Redis, ES, Prometheus, Grafana)
make start-infra

# Wait for services to be healthy, then start applications
make start-services

# Or run everything at once
make docker-up
```

### Generate Events

```bash
# Start synthetic event generation
make start-generator

# Check generator status
make generator-status

# Stop generation
make stop-generator
```

### Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| Analytics API | http://localhost:8080/swagger-ui.html | - |
| Event Producer | http://localhost:8081/actuator | - |
| Stream Processor | http://localhost:8082/actuator | - |
| Grafana | http://localhost:3000 | admin / streaming123 |
| Prometheus | http://localhost:9090 | - |
| Kibana | http://localhost:5601 | - |
| Elasticsearch | http://localhost:9200 | - |

## 📊 API Examples

### Get Dashboard Summary

```bash
curl http://localhost:8080/api/v1/analytics/dashboard?windows=10 | jq .
```

Response:
```json
{
  "totalEvents": 150000,
  "uniqueUsers": 1000,
  "uniqueSessions": 2500,
  "avgLatencyMs": 12.5,
  "eventsByType": {
    "page_view": 45000,
    "click": 37500,
    "scroll": 30000,
    "form_submit": 22500,
    "purchase": 15000
  },
  "eventsByCountry": {
    "US": 60000,
    "UK": 30000,
    "DE": 22500,
    "FR": 15000,
    "JP": 12000,
    "IN": 10500
  },
  "windowsAggregated": 10,
  "computedAt": "2024-01-15T10:30:00Z"
}
```

### Get Recent Metrics

```bash
curl http://localhost:8080/api/v1/analytics/metrics?count=5 | jq .
```

## 📈 Metrics & Monitoring

### Key Prometheus Metrics

| Metric | Description |
|--------|-------------|
| `events_generated_total` | Total synthetic events generated |
| `kafka_events_sent_total` | Events successfully sent to Kafka |
| `kafka_events_processed_total` | Events consumed and processed |
| `kafka_events_duplicates_total` | Duplicate events skipped |
| `aggregation_windows_flushed_total` | Aggregation windows completed |
| `elasticsearch_events_indexed_total` | Events indexed to ES |
| `analytics_queries_executed_total` | API queries executed |
| `analytics_cache_hits_total` | Redis cache hits |

### Grafana Dashboards

Pre-configured dashboard includes:
- Event generation rate (events/sec)
- Processing throughput
- Deduplication rate
- Aggregation window metrics
- ES indexing status
- Cache hit ratio

## ⚙️ Configuration

### Event Producer (`application.yml`)

```yaml
generator:
  events-per-batch: 100      # Events generated per batch
  interval-ms: 100           # Batch interval
  user-pool-size: 1000       # Simulated user pool
```

### Stream Processor

```yaml
aggregation:
  window-size-seconds: 60    # Tumbling window size
  flush-interval-ms: 60000   # Flush interval

deduplication:
  ttl-seconds: 3600          # Dedup key TTL
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka brokers |
| `SPRING_DATA_REDIS_HOST` | localhost | Redis host |
| `ELASTICSEARCH_URL` | http://localhost:9200 | ES endpoint |

## 🧪 Testing

```bash
# Run all tests
make test

# Run with coverage
make test-coverage

# Test specific module
./mvnw -pl event-producer test
```

## 🐳 Docker

### Build Images

```bash
make docker-build
```

### Start/Stop

```bash
# Start all
make docker-up

# Stop all (preserve data)
make docker-down

# Stop and clean volumes
make docker-clean
```

### View Logs

```bash
# All services
make docker-logs

# Specific service
make docker-logs-stream-processor
```

## 📐 Design Decisions

### Why In-Memory Aggregation?

This project uses a simple in-memory windowed aggregator instead of Kafka Streams or Flink for clarity. Production systems should consider:

- **Kafka Streams**: For stateful processing with RocksDB state stores
- **Apache Flink**: For complex event processing and exactly-once guarantees
- **ksqlDB**: For SQL-based stream processing

### Why Redis for Deduplication?

Redis provides:
- O(1) key lookups
- Built-in TTL for automatic cleanup
- Cluster mode for horizontal scaling
- Persistence options (AOF/RDB)

### Why KRaft (No Zookeeper)?

Kafka 3.5+ supports KRaft mode which:
- Eliminates Zookeeper dependency
- Simplifies deployment
- Improves startup time
- Reduces operational complexity

## 🚧 Production Considerations

Before deploying to production:

1. **Security**: Enable TLS, authentication, and authorization
2. **Scaling**: Configure Kafka partitions based on throughput needs
3. **Monitoring**: Add alerting rules in Prometheus/Grafana
4. **Backup**: Configure ES snapshots and Redis persistence
5. **Rate Limiting**: Add API rate limiting
6. **Circuit Breakers**: Implement resilience patterns

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
