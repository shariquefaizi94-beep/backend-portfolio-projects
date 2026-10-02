# Architecture Overview

## System Context

The Workflow Orchestration Platform is a standalone backend system that manages the lifecycle of multi-step business workflows. It receives workflow creation requests via REST API, executes steps asynchronously through Kafka, and provides full observability into workflow state.

## Key Components

### Workflow Engine
The core orchestrator that drives step execution. It:
- Advances workflows through their step pipeline
- Handles success, failure, retry, and compensation
- Enforces state machine invariants via the domain model
- Uses optimistic locking to prevent concurrent modification

### Step Executors
Pluggable step implementations following the Strategy pattern. Each executor:
- Implements the `StepExecutor` interface
- Provides `execute()` for forward logic
- Provides `compensate()` for rollback logic
- Is registered by `StepType` and resolved at runtime

### Kafka Integration
Commands and events flow through Kafka topics:
- `workflow.commands` — triggers step execution, retry, compensation
- `workflow.events` — state change notifications for downstream consumers
- `workflow.events.dlq` — dead letter queue for messages that fail processing

### Idempotency Layer
Redis-based deduplication prevents duplicate processing when Kafka redelivers messages.
Uses a set-if-absent pattern with a configurable TTL.

## Data Model

```
WorkflowInstance (root aggregate)
├── WorkflowStep[] (ordered step pipeline)
└── WorkflowEvent[] (immutable audit trail)
```

## Concurrency Control

Optimistic locking via JPA `@Version` ensures that when multiple Kafka consumers attempt to modify the same workflow, only one succeeds. The others receive an `ObjectOptimisticLockingFailureException` and the message is retried.

## Compensation Strategy

When a step fails after retries are exhausted:
1. The workflow transitions to FAILED
2. A COMPENSATE command is published to Kafka
3. The engine iterates completed steps in reverse order
4. Each step's `compensate()` method undoes its side effects
5. The workflow transitions to COMPENSATED

## Failure Modes

| Failure | Recovery |
|---------|----------|
| Step transient error | Automatic retry (up to max) |
| Step permanent error | Compensation triggered |
| Duplicate Kafka delivery | Idempotency check skips |
| Concurrent modification | Optimistic lock + Kafka retry |
| Kafka producer failure | Logged, manual intervention |
| Database unavailable | Application health check fails, no new workflows accepted |
