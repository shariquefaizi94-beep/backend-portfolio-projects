# ADR-002: Transactional Outbox Pattern

## Status
Accepted

## Context
Publishing events to Kafka after a database change risks losing events if the Kafka send fails after the DB commit. Using Kafka transactions coupled with DB transactions (2PC) is complex and brittle.

## Decision
Use the transactional outbox pattern: write events to an `outbox_events` table in the same DB transaction as the domain change. A background poller reads unsent events and publishes them to Kafka.

## Rationale
- No events lost: event is persisted in same transaction as domain change
- At-least-once delivery: poller retries failed sends
- No distributed transactions needed between DB and Kafka
- Simple implementation compared to CDC (Debezium) while covering the same guarantees

## Consequences
- Small latency between domain change and Kafka publish (polling interval)
- Requires idempotent consumers since events may be published more than once
- Outbox table grows and needs periodic cleanup
