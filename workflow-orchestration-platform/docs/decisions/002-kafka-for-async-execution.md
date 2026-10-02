# ADR-002: Kafka for Asynchronous Step Execution

## Status
Accepted

## Context
Workflow steps could be executed:
1. Synchronously in the API request thread
2. Asynchronously via `@Async` and thread pools
3. Asynchronously via a message broker (Kafka)

## Decision
Use Kafka as the command/event bus for step execution. Each step transition publishes a command to `workflow.commands`, which a consumer picks up to execute the next step.

## Rationale
- Decouples step execution from the API request lifecycle
- Built-in retry semantics via consumer offset management
- Dead letter queue support for unprocessable messages
- Enables future horizontal scaling — multiple consumer instances
- Provides a durable event log for replay and debugging
- Aligns with event-driven architecture patterns used in production systems

## Consequences
- Introduces eventual consistency — API returns before all steps complete
- Requires infrastructure (Kafka broker) for the system to function
- Message ordering within a workflow is guaranteed by using workflowId as the partition key
