package com.nexaforge.workflow.exception;

import java.util.UUID;

/**
 * Thrown when a workflow instance cannot be found by ID or correlation ID.
 */
public class WorkflowNotFoundException extends RuntimeException {

    public WorkflowNotFoundException(UUID workflowId) {
        super("Workflow not found: " + workflowId);
    }

    public WorkflowNotFoundException(String correlationId) {
        super("Workflow not found for correlation ID: " + correlationId);
    }
}
