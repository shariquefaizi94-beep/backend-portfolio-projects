package com.nexaforge.workflow.domain.enums;

/**
 * Status of an individual workflow step.
 */
public enum StepStatus {

    PENDING("Step is waiting to be executed"),
    IN_PROGRESS("Step is currently executing"),
    COMPLETED("Step completed successfully"),
    FAILED("Step execution failed"),
    COMPENSATING("Step compensation is in progress"),
    COMPENSATED("Step has been compensated/rolled back"),
    SKIPPED("Step was skipped due to workflow cancellation");

    private final String description;

    StepStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == COMPENSATED || this == SKIPPED;
    }

    public boolean requiresCompensation() {
        return this == COMPLETED;
    }
}
