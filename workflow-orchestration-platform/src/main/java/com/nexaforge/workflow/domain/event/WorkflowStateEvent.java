package com.nexaforge.workflow.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * State change event published to Kafka after a workflow transition.
 * Used for audit, downstream notification, and event-driven integration.
 */
public record WorkflowStateEvent(
        UUID eventId,
        UUID workflowId,
        String correlationId,
        String previousStatus,
        String newStatus,
        String stepType,
        int stepIndex,
        String detail,
        Instant timestamp
) {
    public static WorkflowStateEvent create(UUID workflowId,
                                            String correlationId,
                                            String previousStatus,
                                            String newStatus,
                                            String stepType,
                                            int stepIndex,
                                            String detail) {
        return new WorkflowStateEvent(
                UUID.randomUUID(),
                workflowId,
                correlationId,
                previousStatus,
                newStatus,
                stepType,
                stepIndex,
                detail,
                Instant.now()
        );
    }
}
