# Observability & Reliability Platform

A comprehensive observability and reliability platform built with Spring Boot, demonstrating production-grade monitoring capabilities including metrics collection, alerting, SLO management, distributed tracing, and health monitoring.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Observability Stack](#observability-stack)
- [Configuration](#configuration)
- [Testing](#testing)
- [Deployment](#deployment)
- [Design Decisions](#design-decisions)

## Features

### 🔍 Metrics Management
- **Metric Definitions**: Register and manage metric definitions (counters, gauges, histograms, timers)
- **Data Point Recording**: Record metric values with labels and timestamps
- **Aggregations**: Calculate average, sum, min, max, percentiles, and rates
- **Statistics**: Comprehensive statistical analysis including standard deviation

### 🚨 Alerting System
- **Alert Rules**: Define alerting rules with expressions, thresholds, and for-duration
- **Alert Lifecycle**: Full lifecycle management (pending → firing → acknowledged → resolved)
- **Severity Levels**: Support for INFO, WARNING, and CRITICAL severity
- **Notification Handlers**: Extensible notification system for alert routing

### 📊 SLO Management
- **SLO Definitions**: Create availability, latency, throughput, and error rate SLOs
- **Error Budget Tracking**: Real-time error budget calculation and burn rate
- **Status Calculation**: Automatic SLI calculation and compliance monitoring
- **At-Risk Detection**: Proactive identification of SLOs at risk of breach

### 🔗 Distributed Tracing
- **Span Management**: Record and query spans with parent-child relationships
- **Trace Assembly**: Reconstruct complete traces from individual spans
- **Latency Analysis**: Calculate percentile latencies and identify slow spans
- **Error Tracking**: Track and analyze error spans by service

### ❤️ Health Monitoring
- **Health Checks**: Register and execute component health checks
- **Service Health**: Aggregate component health into service-level status
- **Uptime Tracking**: Calculate uptime percentages and downtime duration
- **Kubernetes Probes**: Built-in liveness and readiness probe endpoints

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Observability Platform                            │
├─────────────────────────────────────────────────────────────────────┤
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐        │
│  │  Metrics  │  │ Alerting  │  │    SLO    │  │  Tracing  │        │
│  │ Controller│  │ Controller│  │ Controller│  │ Controller│        │
│  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘        │
│        │              │              │              │               │
│  ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐        │
│  │  Metrics  │  │ Alerting  │  │    SLO    │  │  Tracing  │        │
│  │  Service  │  │  Service  │  │  Service  │  │  Service  │        │
│  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘        │
│        │              │              │              │               │
│  ┌─────▼─────────────▼──────────────▼──────────────▼─────┐         │
│  │              In-Memory Data Stores                     │         │
│  │   (Metrics, Alerts, SLOs, Traces, Health)             │         │
│  └────────────────────────────────────────────────────────┘         │
├─────────────────────────────────────────────────────────────────────┤
│                     Scheduled Tasks                                  │
│  • Alert Rule Evaluation (30s)  • SLO Calculation (60s)             │
│  • Data Pruning (1h)            • Prometheus Metrics Update (15s)   │
└─────────────────────────────────────────────────────────────────────┘
```

## Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.2.5 |
| Language | Java 21 |
| Build Tool | Maven |
| Metrics | Micrometer + Prometheus |
| Tracing | Micrometer Tracing |
| Testing | JUnit 5, Mockito, AssertJ |
| Monitoring | Prometheus, Grafana, Jaeger |
| Alerting | Alertmanager |
| Containerization | Docker, Docker Compose |

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose (optional, for full stack)

### Local Development

1. **Clone and build**:
   ```bash
   cd observability-reliability-platform
   mvn clean install
   ```

2. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

3. **Access the application**:
   - Application: http://localhost:8086
   - Health: http://localhost:8086/api/v1/health/live
   - Metrics: http://localhost:8086/actuator/prometheus

### Docker Compose (Full Stack)

1. **Start all services**:
   ```bash
   docker-compose up -d
   ```

2. **Access services**:
   - Application: http://localhost:8086
   - Prometheus: http://localhost:9090
   - Grafana: http://localhost:3000 (admin/admin)
   - Jaeger: http://localhost:16686
   - Alertmanager: http://localhost:9093

3. **Stop services**:
   ```bash
   docker-compose down
   ```

## API Reference

### Metrics API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/metrics/definitions` | POST | Register metric definition |
| `/api/v1/metrics/definitions` | GET | Get all metric definitions |
| `/api/v1/metrics/data` | POST | Record data point |
| `/api/v1/metrics/data/{name}` | GET | Get data points for metric |
| `/api/v1/metrics/data/{name}/avg` | GET | Calculate average |
| `/api/v1/metrics/data/{name}/statistics` | GET | Get comprehensive statistics |

### Alerting API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/alerts/rules` | POST | Create alert rule |
| `/api/v1/alerts/rules` | GET | Get all alert rules |
| `/api/v1/alerts` | GET | Get all alerts |
| `/api/v1/alerts/active` | GET | Get active alerts |
| `/api/v1/alerts/{id}/fire` | POST | Fire an alert |
| `/api/v1/alerts/{id}/resolve` | POST | Resolve an alert |
| `/api/v1/alerts/evaluate` | POST | Evaluate all rules |

### SLO API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/slos` | POST | Create SLO |
| `/api/v1/slos/availability` | POST | Create availability SLO |
| `/api/v1/slos/latency` | POST | Create latency SLO |
| `/api/v1/slos/{id}/status` | GET | Calculate SLO status |
| `/api/v1/slos/{id}/error-budget` | GET | Get error budget summary |
| `/api/v1/slos/status/at-risk` | GET | Get at-risk SLOs |

### Tracing API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/traces/spans` | POST | Record span |
| `/api/v1/traces/{traceId}` | GET | Get complete trace |
| `/api/v1/traces/spans/service/{name}` | GET | Get spans by service |
| `/api/v1/traces/spans/slow` | GET | Get slow spans |
| `/api/v1/traces/spans/errors` | GET | Get error spans |
| `/api/v1/traces/statistics` | GET | Get tracing statistics |

### Health API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/health/live` | GET | Liveness probe |
| `/api/v1/health/ready` | GET | Readiness probe |
| `/api/v1/health` | GET | Get all service health |
| `/api/v1/health/summary` | GET | Get overall health summary |
| `/api/v1/health/{name}/uptime` | GET | Get service uptime |
| `/api/v1/health/unhealthy` | GET | Get unhealthy services |

## Observability Stack

### Prometheus Configuration

The platform exposes metrics at `/actuator/prometheus` with:
- JVM metrics (memory, GC, threads)
- HTTP request metrics (rate, latency, errors)
- Custom observability metrics (active alerts, SLOs, spans)

### Grafana Dashboards

Pre-configured dashboards include:
- **Observability Overview**: Active alerts, SLOs, healthy/unhealthy services
- **HTTP Metrics**: Request rate, latency percentiles
- **JVM Metrics**: Heap memory, threads, GC

### Alert Rules

Pre-configured Prometheus alert rules:
- `ApplicationDown`: Platform unavailable for 1 minute
- `HighErrorRate`: Error rate > 5% for 2 minutes
- `HighLatency`: P95 latency > 1 second
- `HighMemoryUsage`: JVM heap > 85%
- `ManyActiveAlerts`: More than 10 active alerts

## Configuration

### Application Properties

```yaml
server:
  port: 8086

observability:
  alert:
    evaluation-interval: 30000    # 30 seconds
  slo:
    calculation-interval: 60000   # 1 minute
  retention:
    metrics: P7D                  # 7 days
    traces: P1D                   # 1 day
    alerts: P30D                  # 30 days
```

### Profiles

| Profile | Description |
|---------|-------------|
| `default` | Development with DEBUG logging |
| `dev` | Development environment |
| `prod` | Production with reduced logging, longer retention |
| `docker` | Optimized for container deployment |

## Testing

### Run Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=MetricsServiceTest

# With coverage
mvn test jacoco:report
```

### Test Categories

- **Model Tests**: Validation and factory methods
- **Service Tests**: Business logic and state management
- **Integration Tests**: Application context and API endpoints

## Deployment

### Kubernetes Deployment

The platform is designed for Kubernetes deployment with:
- Health probes (liveness/readiness)
- Prometheus annotations for auto-discovery
- Resource limits and JVM container support
- ConfigMap-based configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `default` |
| `JAVA_OPTS` | JVM options | Container-optimized |
| `SERVER_PORT` | Application port | `8086` |

## Design Decisions

### In-Memory Storage
The platform uses in-memory storage with concurrent data structures for simplicity and demonstration purposes. In production, integrate with:
- **Metrics**: Prometheus, InfluxDB, or TimescaleDB
- **Traces**: Jaeger, Zipkin, or Tempo
- **Alerts**: AlertManager with external storage

### Scheduled Evaluation
Alert rules and SLOs are evaluated on scheduled intervals rather than real-time push to:
- Reduce computational overhead
- Support "for duration" semantics
- Enable batch processing of metrics

### Record-based Models
Java records are used for immutable domain models, providing:
- Compile-time immutability
- Auto-generated equals/hashCode/toString
- Compact, readable code

### Error Budget Calculation
SLO error budgets follow Google SRE practices:
- Total budget = 100% - target (e.g., 0.1% for 99.9% target)
- Consumed budget tracks actual vs. allowed errors
- Burn rate indicates budget consumption velocity

## Project Structure

```
observability-reliability-platform/
├── src/main/java/com/portfolio/observability/
│   ├── alerting/          # Alert rules and alerts
│   ├── config/            # Spring configurations
│   ├── health/            # Health monitoring
│   ├── metrics/           # Metrics collection
│   ├── slo/               # SLO management
│   └── tracing/           # Distributed tracing
├── src/test/java/         # Unit tests
├── docker/
│   ├── prometheus/        # Prometheus config
│   ├── grafana/           # Grafana dashboards
│   └── alertmanager/      # Alertmanager config
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## License

This project is part of a portfolio demonstration and is available for educational purposes.

## Author

Backend Portfolio Projects - Demonstrating production-grade Java/Spring Boot development.
