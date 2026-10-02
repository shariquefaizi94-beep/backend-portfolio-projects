package com.nexaforge.workflow.domain.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Represents the lifecycle states of a workflow instance.
 *
 * <p>State machine transitions:
 * <pre>
 *   PENDING → IN_PROGRESS → COMPLETED
 *                ↓
 *             FAILED → COMPENSATING → COMPENSATED
 *                ↓
 *             CANCELLED
 * </pre>
 */
public enum WorkflowStatus {

    PENDING("Workflow created, awaiting execution"),
    IN_PROGRESS("Workflow steps are being executed"),
    COMPLETED("All steps completed successfully"),
    FAILED("One or more steps failed"),
    COMPENSATING("Compensation/rollback in progress"),
    COMPENSATED("Compensation completed successfully"),
    CANCELLED("Workflow was cancelled by user or system");

    private final String description;

    WorkflowStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns the set of valid next states from the current state.
     * Enforces the state machine invariant at the domain level.
     */
    public Set<WorkflowStatus> validTransitions() {
        return switch (this) {
            case PENDING -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> EnumSet.of(COMPLETED, FAILED, CANCELLED);
            case FAILED -> EnumSet.of(COMPENSATING, CANCELLED);
            case COMPENSATING -> EnumSet.of(COMPENSATED, FAILED);
            case COMPLETED, COMPENSATED, CANCELLED -> EnumSet.noneOf(WorkflowStatus.class);
        };
    }

    public boolean canTransitionTo(WorkflowStatus target) {
        return validTransitions().contains(target);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == COMPENSATED || this == CANCELLED;
    }
}
