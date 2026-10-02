# ADR-001: State Machine Enforcement in Domain Model

## Status
Accepted

## Context
Workflow status transitions must be strictly controlled to prevent invalid states (e.g., a COMPLETED workflow transitioning back to IN_PROGRESS). This logic could live in:
1. The service layer (procedural validation)
2. The domain model (rich domain object)
3. A separate state machine library (Spring State Machine, etc.)

## Decision
Enforce state transitions directly in the `WorkflowStatus` enum using `canTransitionTo()` and `validTransitions()` methods. The `WorkflowInstance.transitionTo()` method calls these checks before applying any status change.

## Rationale
- Keeps invariants close to the data — harder to bypass accidentally
- No external library dependency for a straightforward state machine
- Easy to test in isolation (pure enum logic, no Spring context needed)
- Self-documenting: the enum clearly shows all valid transitions

## Consequences
- Adding new states requires updating the transition map
- Complex conditional transitions (e.g., retry limits) are handled outside the enum, in the engine
