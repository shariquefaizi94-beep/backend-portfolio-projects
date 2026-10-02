# Production API Platform

A production-grade API platform demonstrating enterprise patterns including GraphQL APIs, token bucket rate limiting, API gateway pattern, and comprehensive security.

## 🎯 Features

### GraphQL API
- **Full Schema**: Comprehensive GraphQL schema with queries, mutations, and subscriptions
- **Custom Scalars**: UUID, BigDecimal, DateTime support
- **Pagination**: Cursor-based and offset pagination
- **Filtering**: Flexible filtering on all query endpoints
- **Resolvers**: Type-safe resolvers for Products, Orders, and Users

### Rate Limiting (Token Bucket Algorithm)
- **Tiered Limits**: FREE, BASIC, PREMIUM, ENTERPRISE, UNLIMITED tiers
- **Per-User Limits**: Rate limits based on user subscription tier
- **IP-Based Fallback**: Anonymous request limiting by IP address
- **Bucket4j Integration**: Industry-standard token bucket implementation
- **Rate Limit Headers**: X-RateLimit-Limit, X-RateLimit-Remaining, Retry-After

### API Gateway
- **Dynamic Routing**: Route discovery and registration
- **Request Transformation**: Header manipulation, path rewriting
- **Response Transformation**: Response enrichment and standardization
- **Service Discovery**: Simulated backend service routing
- **Health Checking**: Gateway and backend health endpoints

### Security
- **JWT Authentication**: Token-based authentication with configurable expiration
- **API Key Support**: Header-based API key validation
- **Role-Based Access**: User roles (USER, PREMIUM, ADMIN, API_CLIENT)
- **Request Filtering**: Security filters for authentication

### Caching & Performance
- **Caffeine Cache**: High-performance in-memory caching
- **Cache-Aside Pattern**: Automatic cache population and eviction
- **Response Caching**: Configurable TTL for different endpoints

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │   Routing   │  │ Rate Limit  │  │    Request Transform    │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└──────────────────────────────┬──────────────────────────────────┘
                               │
            ┌──────────────────┴───────────────────┐
            │                                       │
    ┌───────┴───────┐                     ┌────────┴────────┐
    │  GraphQL API  │                     │    REST API     │
    │   /graphql    │                     │   /gateway/*    │
    └───────┬───────┘                     └────────┬────────┘
            │                                       │
    ┌───────┴───────────────────────────────────────┴───────┐
    │                    Service Layer                       │
    │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
    │  │ProductService│ │OrderService │  │ UserService │   │
    │  └─────────────┘  └─────────────┘  └─────────────┘   │
    └───────────────────────────┬───────────────────────────┘
                               │
    ┌───────────────────────────┴───────────────────────────┐
    │                  Repository Layer                      │
    │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
    │  │   Product   │  │    Order    │  │    User     │   │
    │  │ Repository  │  │ Repository  │  │ Repository  │   │
    │  └─────────────┘  └─────────────┘  └─────────────┘   │
    └────────────────────────────────────────────────────────┘
```

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker (optional, for containerized deployment)

### Build
```bash
cd production-api-platform
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

### Test
```bash
mvn test
```

### Docker
```bash
cd docker
docker-compose up -d
```

## 📡 API Endpoints

### GraphQL
- **GraphQL Endpoint**: `POST /graphql`
- **GraphiQL UI**: `GET /graphiql` (development only)

### Gateway
- **Route Request**: `ANY /gateway/api/{service}/**`
- **Gateway Health**: `GET /gateway/health`
- **List Routes**: `GET /gateway/routes`

### Monitoring
- **Health**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Prometheus**: `GET /actuator/prometheus`

## 📊 GraphQL Examples

### Query Products
```graphql
query {
  products(filter: { category: "Electronics" }, page: 0, size: 10) {
    products {
      id
      name
      price
      stockQuantity
      available
    }
    pageInfo {
      totalElements
      hasNext
    }
  }
}
```

### Create Order
```graphql
mutation {
  createOrder(input: {
    userId: "550e8400-e29b-41d4-a716-446655440000"
    items: [
      { productId: "...", quantity: 2 }
    ]
    shippingAddress: "123 Main St"
    billingAddress: "123 Main St"
  }) {
    id
    total
    status
  }
}
```

### Search Products
```graphql
query {
  searchProducts(searchTerm: "iPhone", limit: 5) {
    id
    name
    price
    category
  }
}
```

## 🔐 Rate Limiting

| Tier       | Requests/Min | Requests/Day |
|------------|--------------|--------------|
| FREE       | 60           | 1,000        |
| BASIC      | 120          | 5,000        |
| PREMIUM    | 300          | 20,000       |
| ENTERPRISE | 1,000        | 100,000      |
| UNLIMITED  | ∞            | ∞            |

### Headers
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Tier: FREE
Retry-After: 30  (when rate limited)
```

## 🛠️ Technology Stack

| Component          | Technology                    |
|--------------------|-------------------------------|
| Framework          | Spring Boot 3.2.5             |
| GraphQL            | Spring GraphQL                |
| Rate Limiting      | Bucket4j                      |
| JWT                | jjwt-api                      |
| Caching            | Caffeine                      |
| Resilience         | Resilience4j                  |
| Metrics            | Micrometer + Prometheus       |
| Build              | Maven                         |
| Container          | Docker                        |

## 📈 Metrics & Monitoring

### Available Metrics
- `api.ratelimit.accepted` - Accepted requests count
- `api.ratelimit.rejected` - Rejected requests count
- `gateway.request.latency` - Gateway latency histogram
- Standard JVM and Spring Boot metrics

### Grafana Dashboards
Pre-configured dashboards available in `docker/grafana/provisioning/`

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=RateLimiterServiceTest
```

## 📁 Project Structure

```
production-api-platform/
├── src/
│   ├── main/
│   │   ├── java/com/portfolio/api/
│   │   │   ├── domain/model/      # Domain entities
│   │   │   ├── repository/        # Data access layer
│   │   │   ├── service/           # Business logic
│   │   │   ├── graphql/           # GraphQL resolvers
│   │   │   ├── gateway/           # API Gateway
│   │   │   ├── ratelimit/         # Rate limiting
│   │   │   ├── security/          # Authentication
│   │   │   ├── config/            # Configuration
│   │   │   └── exception/         # Error handling
│   │   └── resources/
│   │       ├── graphql/           # GraphQL schema
│   │       └── application.yml
│   └── test/                      # Test classes
├── docker/                        # Docker configuration
└── docs/                          # Documentation
```

## 📄 License

This project is part of a portfolio demonstration and is licensed under the MIT License.
