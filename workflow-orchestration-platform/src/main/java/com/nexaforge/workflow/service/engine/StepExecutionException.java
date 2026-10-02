package com.nexaforge.workflow.service.engine;

/**
 * Thrown when a workflow step execution fails.
 * Carries information about whether the failure is retryable.
 */
public class StepExecutionException extends RuntimeException {

    private final boolean retryable;

    public StepExecutionException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public StepExecutionException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
