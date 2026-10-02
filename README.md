# Backend Engineering Portfolio

[![Build](https://github.com/shariquefaizi94-beep/backend-portfolio-projects/actions/workflows/build.yml/badge.svg)](https://github.com/shariquefaizi94-beep/backend-portfolio-projects/actions/workflows/build.yml)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Production-quality backend engineering portfolio demonstrating distributed systems, microservices, event-driven architecture, and cloud-native patterns. All projects are original code designed to showcase senior/staff-level backend engineering skills.

## 🏗️ Projects Overview

| # | Project | Description | Tests | Key Technologies |
|---|---------|-------------|-------|------------------|
| 1 | [Workflow Orchestration Platform](workflow-orchestration-platform/) | Distributed state machine with saga patterns | 13 | State Machines, Kafka, PostgreSQL |
| 2 | [Event-Driven Order Platform](event-driven-order-platform/) | Microservices with choreography saga | 8 | Kafka, Transactional Outbox, Redis |
| 3 | [Real-Time Streaming Analytics](realtime-streaming-analytics/) | High-throughput event processing | 68 | Kafka, Elasticsearch, Windowed Aggregation |
| 4 | [Production API Platform](production-api-platform/) | API Gateway with GraphQL & rate limiting | 69 | GraphQL/DGS, JWT, Rate Limiting, API Gateway |
| 5 | [Cloud-Native Microservices](cloud-native-microservices/) | Kubernetes-ready microservices platform | 47 | Kubernetes, Istio, OpenFeign, Kustomize |
| 6 | [Observability & Reliability Platform](observability-reliability-platform/) | Full observability stack with SLOs | 164 | Prometheus, Grafana, Jaeger, SLO Management |

**Total: 369 tests across 6 production-quality projects**

## ✨ Skills Demonstrated

### Architecture & Design
- Distributed system design patterns
- Event-driven architecture (EDA)
- CQRS and Event Sourcing
- Saga patterns (Orchestration & Choreography)
- Domain-Driven Design (DDD)
- API Gateway patterns
- Service mesh architecture
- SRE practices (SLOs, Error Budgets)

### Backend Technologies
- **Languages**: Java 21 (Records, Virtual Threads, Pattern Matching)
- **Frameworks**: Spring Boot 3.x, Spring Data, Spring Kafka, Spring Cloud OpenFeign
- **Messaging**: Apache Kafka (KRaft mode), Redis Pub/Sub
- **Databases**: PostgreSQL, Redis, Elasticsearch
- **API Design**: REST, OpenAPI 3.0, GraphQL (DGS Framework)
- **Security**: JWT Authentication, API Key Validation, Rate Limiting

### DevOps & Infrastructure
- Docker & Docker Compose
- Kubernetes manifests (Deployment, Service, HPA, PDB)
- Kustomize overlays (dev/prod environments)
- Istio VirtualService for traffic management
- GitHub Actions CI/CD
- Infrastructure as Code

### Observability & Reliability
- Prometheus metrics collection
- Grafana dashboards (pre-configured)
- Distributed tracing (Jaeger, OpenTelemetry)
- Alertmanager with alert rules
- SLO/SLI management with error budgets
- Health check orchestration
- Structured logging (JSON)

## 📁 Repository Structure

```
backend-portfolio-projects/
├── workflow-orchestration-platform/     # Project 1: State Machine Engine
│   ├── src/main/java/...
│   ├── docker/
│   └── README.md
├── event-driven-order-platform/         # Project 2: E-Commerce Microservices
│   ├── order-service/
│   ├── inventory-service/
│   ├── payment-service/
│   ├── shipping-service/
│   ├── notification-service/
│   └── common-events/
├── realtime-streaming-analytics/        # Project 3: Stream Processing
│   ├── event-producer/
│   ├── stream-processor/
│   └── analytics-api/
├── production-api-platform/             # Project 4: API Gateway & GraphQL
│   ├── src/main/java/.../gateway/       # API Gateway
│   ├── src/main/java/.../graphql/       # GraphQL Resolvers
│   ├── src/main/java/.../ratelimit/     # Rate Limiting
│   ├── src/main/java/.../security/      # JWT & API Keys
│   └── docker/
├── cloud-native-microservices/          # Project 5: K8s Platform
│   ├── src/main/java/.../product/       # Product Service
│   ├── src/main/java/.../user/          # User Service
│   ├── src/main/java/.../inventory/     # Inventory Service
│   ├── src/main/java/.../notification/  # Notification Service
│   ├── k8s/base/                        # K8s manifests
│   ├── k8s/overlays/                    # Kustomize overlays
│   └── docker/
├── observability-reliability-platform/  # Project 6: Observability Stack
│   ├── src/main/java/.../metrics/       # Metrics Collection
│   ├── src/main/java/.../tracing/       # Distributed Tracing
│   ├── src/main/java/.../alerting/      # Alert Management
│   ├── src/main/java/.../slo/           # SLO Management
│   ├── src/main/java/.../health/        # Health Checks
│   ├── docker/prometheus/               # Prometheus config
│   ├── docker/grafana/                  # Grafana dashboards
│   └── docker/alertmanager/             # Alertmanager config
├── pom.xml                              # Parent POM
└── README.md
```

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose (optional, for full stack)

### Build All Projects

```bash
# Clone the repository
git clone https://github.com/shariquefaizi94-beep/backend-portfolio-projects.git
cd backend-portfolio-projects

# Build all modules
mvn clean install

# Run all tests
mvn test
```

### Run Individual Projects

Each project can run standalone or with Docker Compose for the full stack:

```bash
# Project 1: Workflow Orchestration
cd workflow-orchestration-platform
mvn spring-boot:run

# Project 4: Production API Platform
cd production-api-platform
mvn spring-boot:run
# Access GraphQL Playground at http://localhost:8080/graphiql

# Project 5: Cloud-Native Microservices
cd cloud-native-microservices
mvn spring-boot:run

# Project 6: Observability Platform (with full stack)
cd observability-reliability-platform
docker-compose up -d
# Access: App (8080), Prometheus (9090), Grafana (3000), Jaeger (16686)
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

**Test Coverage**: 13 unit tests

---

### Project 2: Event-Driven Order Platform

**Problem**: E-commerce order processing requires coordination across inventory, payment, and shipping services.

**Solution**: Microservices architecture with:
- Choreography-based saga pattern
- Transactional outbox for reliable messaging
- Eventually consistent distributed transactions
- Automatic compensation on failures

**Modules**: 6 (5 services + common-events)  
**Test Coverage**: 8 unit tests

---

### Project 3: Real-Time Streaming Analytics

**Problem**: Process high-volume clickstream data with sub-second latency for real-time dashboards.

**Solution**: Stream processing pipeline with:
- 100K+ events/second ingestion
- Tumbling window aggregations
- Redis-based deduplication
- Elasticsearch for analytics storage

**Modules**: 3 (event-producer, stream-processor, analytics-api)  
**Test Coverage**: 68 unit tests

---

### Project 4: Production API Platform

**Problem**: Production APIs need robust gateway functionality, flexible querying, and protection against abuse.

**Solution**: Enterprise API platform with:
- **API Gateway**: Route management, request/response transformation
- **GraphQL**: Product, Order, User resolvers with DGS framework
- **Rate Limiting**: Tiered limits (Basic: 100/min, Pro: 500/min, Enterprise: 2000/min)
- **Security**: JWT authentication, API key validation
- **Caching**: In-memory caching for performance

**Test Coverage**: 69 unit tests

---

### Project 5: Cloud-Native Microservices

**Problem**: Modern applications need to be cloud-native with proper orchestration, resilience, and deployment patterns.

**Solution**: Kubernetes-ready microservices platform with:
- **Services**: Product, User, Inventory, Notification
- **Service Communication**: OpenFeign clients with resilience fallbacks
- **Kubernetes Manifests**: Deployment, Service, HPA, PDB
- **Traffic Management**: Istio VirtualService for canary deployments
- **Environment Management**: Kustomize overlays for dev/prod

**Test Coverage**: 47 unit tests

---

### Project 6: Observability & Reliability Platform

**Problem**: Production systems need comprehensive observability for debugging, performance analysis, and reliability.

**Solution**: Full observability platform with:
- **Metrics**: Custom metric definitions, data point collection, aggregations
- **Tracing**: Distributed tracing with span management and service maps
- **SLOs**: Service Level Objectives with error budget tracking
- **Alerting**: Alert rules, incident management, notifications
- **Health**: Health check orchestration with component monitoring
- **Stack**: Prometheus, Grafana (with dashboards), Jaeger, Alertmanager

**Test Coverage**: 164 unit tests

## 🛠️ Tech Stack Summary

| Category | Technologies |
|----------|--------------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.2.5 |
| **GraphQL** | Netflix DGS Framework |
| **Messaging** | Apache Kafka 7.6.0 (KRaft) |
| **Databases** | PostgreSQL 16, Redis 7.2, Elasticsearch 8.13 |
| **Monitoring** | Prometheus, Grafana, Jaeger, Alertmanager |
| **Tracing** | Micrometer Tracing, OpenTelemetry |
| **Cloud Native** | Kubernetes, Istio, Kustomize |
| **Build** | Maven 3.9.x |
| **Containers** | Docker, Docker Compose |
| **CI/CD** | GitHub Actions |

## 🧪 Testing

```bash
# Run all tests across all projects
mvn test

# Run tests for specific project
mvn test -pl workflow-orchestration-platform
mvn test -pl production-api-platform
mvn test -pl cloud-native-microservices
mvn test -pl observability-reliability-platform

# Run with coverage
mvn test jacoco:report
```

## 📈 Portfolio Metrics

| Metric | Value |
|--------|-------|
| Total Source Files | 300+ |
| Total Lines of Code | 25,000+ |
| Total Unit Tests | 369 |
| Projects Completed | 6/6 ✅ |

## 🎯 Architecture Highlights

### API Gateway Pattern (Project 4)
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│ API Gateway │────▶│  Services   │
└─────────────┘     │ • Routing   │     │ • Products  │
                    │ • Auth      │     │ • Orders    │
                    │ • Rate Limit│     │ • Users     │
                    └─────────────┘     └─────────────┘
```

### Cloud-Native Architecture (Project 5)
```
┌─────────────────────────────────────────────────────┐
│                  Kubernetes Cluster                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐          │
│  │ Product  │  │   User   │  │Inventory │          │
│  │ Service  │◀─│ Service  │─▶│ Service  │          │
│  └──────────┘  └──────────┘  └──────────┘          │
│       │              │              │               │
│       └──────────────┼──────────────┘               │
│                      ▼                              │
│              ┌──────────────┐                       │
│              │ Notification │                       │
│              │   Service    │                       │
│              └──────────────┘                       │
└─────────────────────────────────────────────────────┘
```

### Observability Stack (Project 6)
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Application │────▶│ Prometheus  │────▶│   Grafana   │
│  Metrics    │     │  (Scrape)   │     │ (Dashboard) │
└─────────────┘     └─────────────┘     └─────────────┘
       │
       ▼
┌─────────────┐     ┌─────────────┐
│   Traces    │────▶│   Jaeger    │
│  (Spans)    │     │ (Tracing)   │
└─────────────┘     └─────────────┘
       │
       ▼
┌─────────────┐     ┌─────────────┐
│   Alerts    │────▶│Alertmanager │
│  (Rules)    │     │(Notifications)│
└─────────────┘     └─────────────┘
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Sharique Faizi**
- GitHub: [@shariquefaizi94-beep](https://github.com/shariquefaizi94-beep)

---

*This portfolio demonstrates production-quality backend engineering skills for senior/staff-level positions. All code is original and follows industry best practices.*
