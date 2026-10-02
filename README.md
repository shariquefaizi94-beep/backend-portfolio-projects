# Backend Engineering Portfolio

[![Build](https://github.com/shariquefaizi94-beep/backend-portfolio-projects/actions/workflows/build.yml/badge.svg)](https://github.com/shariquefaizi94-beep/backend-portfolio-projects/actions/workflows/build.yml)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Production-quality backend engineering portfolio demonstrating distributed systems, microservices, event-driven architecture, and cloud-native patterns. All projects are original code designed to showcase senior/staff-level backend engineering skills.

## 🏗️ Projects Overview

| # | Project | Description | Key Technologies |
|---|---------|-------------|------------------|
| 1 | [Workflow Orchestration Platform](workflow-orchestration-platform/) | Distributed state machine with saga patterns | State Machines, Kafka, PostgreSQL |
| 2 | [Event-Driven Order Platform](event-driven-order-platform/) | Microservices with choreography saga | Kafka, Transactional Outbox, Redis |
| 3 | [Real-Time Streaming Analytics](realtime-streaming-analytics/) | High-throughput event processing | Kafka, Elasticsearch, Windowed Aggregation |
| 4 | Production API Platform | *(Coming Soon)* | GraphQL, Rate Limiting, API Gateway |
| 5 | Cloud-Native Microservices | *(Coming Soon)* | Kubernetes, Service Mesh, GitOps |
| 6 | Observability Platform | *(Coming Soon)* | Prometheus, Grafana, Distributed Tracing |

## ✨ Skills Demonstrated

### Architecture & Design
- Distributed system design patterns
- Event-driven architecture (EDA)
- CQRS and Event Sourcing
- Saga patterns (Orchestration & Choreography)
- Domain-Driven Design (DDD)

### Backend Technologies
- **Languages**: Java 21 (Records, Virtual Threads, Pattern Matching)
- **Frameworks**: Spring Boot 3.x, Spring Data, Spring Kafka
- **Messaging**: Apache Kafka (KRaft mode), Redis Pub/Sub
- **Databases**: PostgreSQL, Redis, Elasticsearch
- **API Design**: REST, OpenAPI 3.0, GraphQL

### DevOps & Infrastructure
- Docker & Docker Compose
- Kubernetes manifests
- GitHub Actions CI/CD
- Infrastructure as Code

### Observability
- Prometheus metrics
- Grafana dashboards
- Distributed tracing (OpenTelemetry)
- Structured logging (JSON)

## 📁 Repository Structure

```
backend-portfolio-projects/
├── workflow-orchestration-platform/     # Project 1
│   ├── src/main/java/...
│   ├── docker/
│   └── docs/
├── event-driven-order-platform/         # Project 2
│   ├── order-service/
│   ├── inventory-service/
│   ├── payment-service/
│   ├── shipping-service/
│   ├── notification-service/
│   ├── common/
│   ├── outbox-processor/
│   └── docker/
├── realtime-streaming-analytics/        # Project 3
│   ├── event-producer/
│   ├── stream-processor/
│   ├── analytics-api/
│   └── docker/
├── production-api-platform/             # Project 4 (TODO)
├── cloud-native-microservices/          # Project 5 (TODO)
├── observability-reliability-platform/  # Project 6 (TODO)
├── pom.xml                              # Parent POM
├── Makefile
└── README.md
```

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose

### Build All Projects

```bash
# Clone the repository
git clone https://github.com/shariquefaizi94-beep/backend-portfolio-projects.git
cd backend-portfolio-projects

# Build all modules
make build

# Run all tests
make test
```

### Run Individual Projects

Each project has its own Docker Compose setup for running locally:

```bash
# Project 1: Workflow Orchestration
cd workflow-orchestration-platform
make docker-up

# Project 2: Event-Driven Orders
cd event-driven-order-platform
make docker-up

# Project 3: Streaming Analytics
cd realtime-streaming-analytics
make docker-up
```

## 📊 Project Details

### Project 1: Workflow Orchestration Platform

**Problem**: Complex business processes require coordinated state transitions across multiple services with failure handling.

**Solution**: A distributed workflow engine implementing:
- State machine lifecycle management
- Idempotent command processing
- Compensation/rollback via saga
- Retry with exponential backoff
- Dead letter queue handling
- Full observability

**Test Coverage**: 13 unit tests

---

### Project 2: Event-Driven Order Platform

**Problem**: E-commerce order processing requires coordination across inventory, payment, and shipping services.

**Solution**: Microservices architecture with:
- Choreography-based saga pattern
- Transactional outbox for reliable messaging
- Eventually consistent distributed transactions
- Automatic compensation on failures
- Redis caching for performance

**Modules**: 7 (5 services + common + outbox-processor)  
**Test Coverage**: 8 unit tests

---

### Project 3: Real-Time Streaming Analytics

**Problem**: Process high-volume clickstream data with sub-second latency for real-time dashboards.

**Solution**: Stream processing pipeline with:
- 100K+ events/second ingestion
- Tumbling window aggregations
- Redis-based deduplication
- Elasticsearch for analytics storage
- Pre-built Grafana dashboards

**Modules**: 3 (event-producer, stream-processor, analytics-api)  
**Test Coverage**: 68 unit tests

## 🛠️ Tech Stack Summary

| Category | Technologies |
|----------|--------------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.2.5 |
| **Messaging** | Apache Kafka 7.6.0 (KRaft) |
| **Databases** | PostgreSQL 16, Redis 7.2, Elasticsearch 8.13 |
| **Monitoring** | Prometheus, Grafana, OpenTelemetry |
| **Build** | Maven 3.9.x |
| **Containers** | Docker, Docker Compose |
| **CI/CD** | GitHub Actions |

## 🧪 Testing

```bash
# Run all tests across all projects
make test

# Run tests for specific project
make test-workflow
make test-orders
make test-streaming

# Run with coverage
make test-coverage
```

## 📈 Metrics

| Metric | Value |
|--------|-------|
| Total Source Files | 180+ |
| Total Lines of Code | 15,000+ |
| Total Unit Tests | 89+ |
| Projects Completed | 3/6 |

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Sharique Faizi**
- GitHub: [@shariquefaizi94-beep](https://github.com/shariquefaizi94-beep)

---

*This portfolio demonstrates production-quality backend engineering skills. All code is original and designed for educational purposes.*
