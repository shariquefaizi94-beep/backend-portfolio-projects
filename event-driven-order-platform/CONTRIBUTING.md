# Contributing

## Development Setup
1. Install Java 21+, Maven 3.9+, Docker
2. `make infra-up` to start Postgres, Kafka, Prometheus, Grafana
3. `make build` to compile all modules
4. `make test` to run unit tests
5. Start services individually via `mvn spring-boot:run` in each module

## Code Standards
- Every consumer must be idempotent
- Every saga step needs a compensation handler
- Use the transactional outbox for event publishing from order-service
- Use correlation IDs for cross-service tracing
