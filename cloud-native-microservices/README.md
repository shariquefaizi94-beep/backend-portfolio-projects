# Cloud Native Microservices Platform

A production-ready cloud-native microservices platform demonstrating Kubernetes deployment patterns, service mesh configuration, distributed tracing, and resilience patterns.

## 🎯 Features

### Microservices Architecture
- **User Service**: User management with CRUD operations
- **Product Service**: Product catalog management
- **Inventory Service**: Stock management with reservations
- **Notification Service**: Multi-channel notification delivery

### Kubernetes Patterns
- **Deployments**: Rolling updates, pod anti-affinity
- **Services**: ClusterIP, Headless services
- **ConfigMaps**: Externalized configuration
- **HPA**: Horizontal Pod Autoscaler with CPU/Memory metrics
- **PDB**: Pod Disruption Budget for high availability
- **Probes**: Liveness, Readiness, and Startup probes
- **RBAC**: Service accounts with minimal permissions

### Service Mesh (Istio)
- **VirtualService**: Traffic routing, retries, timeouts
- **DestinationRule**: Load balancing, circuit breaking
- **Gateway**: External traffic ingress

### Resilience Patterns
- **Circuit Breaker**: Resilience4j with configurable thresholds
- **Retry**: Exponential backoff retry strategy
- **Fallback**: Graceful degradation on failures
- **Bulkhead**: (via Istio connection pools)

### Distributed Tracing
- **Micrometer Tracing**: Spring Boot integration
- **Zipkin**: Trace collection and visualization
- **Trace Propagation**: W3C Trace Context

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    Istio Ingress Gateway                     │
└────────────────────────────┬─────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────────┐
│                     Istio Service Mesh                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐   │
│  │ User Service│  │Product Svc  │  │  Inventory Service  │   │
│  │  + Envoy    │  │  + Envoy    │  │     + Envoy         │   │
│  └─────────────┘  └─────────────┘  └─────────────────────┘   │
│         │                │                    │               │
│         └────────────────┼────────────────────┘               │
│                          │                                    │
│              ┌───────────┴───────────┐                       │
│              │ Notification Service  │                       │
│              │      + Envoy          │                       │
│              └───────────────────────┘                       │
└──────────────────────────────────────────────────────────────┘
                             │
    ┌────────────────────────┼─────────────────────────┐
    │           Observability Stack                     │
    │  ┌─────────┐  ┌──────────┐  ┌────────────────┐  │
    │  │ Zipkin  │  │Prometheus│  │    Grafana     │  │
    │  │ Tracing │  │ Metrics  │  │ Visualization  │  │
    │  └─────────┘  └──────────┘  └────────────────┘  │
    └──────────────────────────────────────────────────┘
```

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker (optional)
- Kubernetes cluster (optional)
- kubectl + kustomize (optional)

### Build
```bash
cd cloud-native-microservices
mvn clean install
```

### Run Locally
```bash
mvn spring-boot:run
```

### Run Tests
```bash
mvn test
```

### Docker
```bash
cd docker
docker-compose up -d
```

### Deploy to Kubernetes
```bash
# Development environment
kubectl apply -k k8s/overlays/dev

# Production environment
kubectl apply -k k8s/overlays/prod
```

## 📡 API Endpoints

### User Service
- `GET /api/users` - List all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create user
- `PUT /api/users/{id}/status` - Update user status
- `DELETE /api/users/{id}` - Delete user

### Product Service
- `GET /api/products` - List all products
- `GET /api/products/{id}` - Get product by ID
- `GET /api/products/sku/{sku}` - Get product by SKU
- `POST /api/products` - Create product
- `PUT /api/products/{id}/price` - Update price
- `DELETE /api/products/{id}` - Delete product

### Inventory Service
- `GET /api/inventory/product/{productId}` - Get inventory
- `GET /api/inventory/product/{productId}/available` - Get available quantity
- `POST /api/inventory/product/{productId}/reserve` - Reserve stock
- `POST /api/inventory/product/{productId}/release` - Release reservation
- `POST /api/inventory/product/{productId}/restock` - Restock

### Health Endpoints
- `GET /health/live` - Liveness probe
- `GET /health/ready` - Readiness probe
- `GET /health/detailed` - Detailed health info

## ☸️ Kubernetes Configuration

### Deployment Features
- Rolling update strategy
- Resource requests and limits
- Pod anti-affinity for distribution
- Graceful shutdown (30s termination period)
- ConfigMap for externalized configuration

### Horizontal Pod Autoscaler
- Min replicas: 2 (dev: 1, prod: 3)
- Max replicas: 10 (prod: 20)
- Scale on CPU (70%) and Memory (80%)
- Scale down stabilization: 300s
- Scale up stabilization: 60s

### Istio Configuration
- Request retries: 3 attempts, 10s timeout each
- Circuit breaker: 3 consecutive 5xx errors
- Connection pool: 100 TCP, 1000 HTTP/2 requests
- Outlier detection: 30s ejection time

## 🔧 Configuration

### Resilience4j Circuit Breaker
| Parameter | Value |
|-----------|-------|
| Sliding window size | 10 |
| Failure rate threshold | 50% |
| Wait duration in open | 5s |
| Permitted calls in half-open | 3 |

### Retry Configuration
| Parameter | Value |
|-----------|-------|
| Max attempts | 3 |
| Wait duration | 500ms |
| Exponential backoff | 2x multiplier |

## 📊 Observability

### Metrics
- HTTP server request metrics
- Circuit breaker metrics
- JVM metrics
- Custom business metrics

### Tracing
- Automatic trace context propagation
- 100% sampling rate (configurable)
- Zipkin integration

### Endpoints
- `/actuator/health` - Health info
- `/actuator/prometheus` - Prometheus metrics
- `/actuator/circuitbreakers` - Circuit breaker status

## 📁 Project Structure

```
cloud-native-microservices/
├── src/main/java/com/portfolio/cloudnative/
│   ├── user/           # User microservice
│   ├── product/        # Product microservice
│   ├── inventory/      # Inventory microservice
│   ├── notification/   # Notification microservice
│   └── common/         # Shared components
├── src/main/resources/
│   └── application.yml # Configuration
├── src/test/           # Test classes
├── k8s/                # Kubernetes manifests
│   ├── base/           # Base configuration
│   └── overlays/       # Environment overrides
│       ├── dev/
│       └── prod/
└── docker/             # Docker configuration
```

## 🛠️ Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.2.5 |
| Cloud | Spring Cloud 2023.0.1 |
| Resilience | Resilience4j 2.2.0 |
| Tracing | Micrometer Tracing + Zipkin |
| Service Mesh | Istio |
| Container | Kubernetes |
| Build | Maven |

## 📄 License

This project is part of a portfolio demonstration and is licensed under the MIT License.
