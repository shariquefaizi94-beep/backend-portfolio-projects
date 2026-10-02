package com.nexaforge.workflow.exception;

/**
 * Thrown when a workflow with the same correlation ID already exists.
 * This supports idempotent workflow creation — the caller can safely
 * retry and will get the existing workflow back.
 */
public class DuplicateWorkflowException extends RuntimeException {

    private final String correlationId;

    public DuplicateWorkflowException(String correlationId) {
        super("Workflow already exists for correlation ID: " + correlationId);
        this.correlationId = correlationId;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
