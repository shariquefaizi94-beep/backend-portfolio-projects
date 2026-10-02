# ADR-003: Optimistic Locking for Concurrent Access

## Status
Accepted

## Context
Multiple Kafka consumers or retry attempts could modify the same workflow instance simultaneously. Without concurrency control, this leads to lost updates.

## Decision
Use JPA `@Version` annotation on `WorkflowInstance` for optimistic locking. Concurrent modifications cause `ObjectOptimisticLockingFailureException`, which propagates to the Kafka consumer, triggering a message retry.

## Rationale
- No distributed locks needed — database-level concurrency control
- Works naturally with Kafka's at-least-once delivery semantics
- Failed attempts are automatically retried via Kafka consumer mechanism
- Simpler than pessimistic locking and no deadlock risk

## Consequences
- Under high contention, multiple retry cycles may occur before a consumer succeeds
- The idempotency layer prevents duplicate side effects during retries
