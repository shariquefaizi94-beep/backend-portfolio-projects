# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2024-01-15

### Added
- Core workflow orchestration engine with state machine lifecycle
- Order fulfillment workflow (validate → pay → reserve → ship → notify → complete)
- Kafka-based asynchronous command/event processing
- Redis-backed idempotency service
- Saga compensation pattern with reverse-order rollback
- Optimistic locking for concurrent workflow access
- Dead letter queue for failed messages
- PostgreSQL persistence with Flyway migrations
- REST API with OpenAPI/Swagger documentation
- Prometheus metrics and Grafana dashboard support
- Docker Compose for local development stack
- GitHub Actions CI/CD pipeline
- Unit tests with JUnit 5 and Mockito
- RFC 7807 Problem Detail error responses
