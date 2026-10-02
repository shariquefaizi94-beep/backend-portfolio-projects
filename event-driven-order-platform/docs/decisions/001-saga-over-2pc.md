# ADR-001: Saga Pattern over Two-Phase Commit

## Status
Accepted

## Context
Multi-service transactions need consistency. Options: 2PC (distributed transactions) or Saga (compensating transactions).

## Decision
Use choreography-based Saga pattern. Each service publishes events to Kafka after completing its work. The order-service acts as the saga coordinator, listening for events and advancing the order state.

## Rationale
- 2PC requires all services to be available simultaneously — poor fault tolerance
- Saga allows services to operate independently with eventual consistency
- Compensation handlers provide explicit rollback logic per service
- Aligns with microservices best practices and real-world systems

## Consequences
- Eventual consistency: order state may temporarily be inconsistent across services
- Requires idempotent consumers to handle duplicate Kafka deliveries
- Compensation logic must be carefully designed for each step
