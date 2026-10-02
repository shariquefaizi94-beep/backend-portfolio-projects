package com.nexaforge.workflow.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Command event published to Kafka to trigger workflow step execution.
 * Carries all context needed for a consumer to execute the step
 * without requiring a database lookup first.
 */
public record WorkflowCommandEvent(
        UUID eventId,
        UUID workflowId,
        String correlationId,
        String commandType,
        String stepType,
        int stepIndex,
        String payload,
        int attemptNumber,
        Instant timestamp
) {
    public static WorkflowCommandEvent create(UUID workflowId,
                                              String correlationId,
                                              String commandType,
                                              String stepType,
                                              int stepIndex,
                                              String payload,
                                              int attemptNumber) {
        return new WorkflowCommandEvent(
                UUID.randomUUID(),
                workflowId,
                correlationId,
                commandType,
                stepType,
                stepIndex,
                payload,
                attemptNumber,
                Instant.now()
        );
    }
}
