package com.nexaforge.workflow.controller.dto;

import com.nexaforge.workflow.domain.model.WorkflowInstance;
import com.nexaforge.workflow.domain.model.WorkflowStep;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for workflow details.
 */
public record WorkflowResponse(
        UUID id,
        String correlationId,
        String workflowType,
        String status,
        int currentStepIndex,
        int retryCount,
        int maxRetries,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        List<StepResponse> steps,
        List<EventResponse> events
) {

    public static WorkflowResponse from(WorkflowInstance workflow) {
        List<StepResponse> steps = workflow.getSteps().stream()
                .map(StepResponse::from)
                .toList();

        List<EventResponse> events = workflow.getEvents().stream()
                .map(EventResponse::from)
                .toList();

        return new WorkflowResponse(
                workflow.getId(),
                workflow.getCorrelationId(),
                workflow.getWorkflowType(),
                workflow.getStatus().name(),
                workflow.getCurrentStepIndex(),
                workflow.getRetryCount(),
                workflow.getMaxRetries(),
                workflow.getFailureReason(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt(),
                workflow.getCompletedAt(),
                steps,
                events
        );
    }

    public record StepResponse(
            UUID id,
            String stepType,
            String status,
            int stepOrder,
            int retryCount,
            String errorMessage,
            Long durationMs,
            Instant createdAt,
            Instant completedAt
    ) {
        public static StepResponse from(WorkflowStep step) {
            return new StepResponse(
                    step.getId(),
                    step.getStepType().name(),
                    step.getStatus().name(),
                    step.getStepOrder(),
                    step.getRetryCount(),
                    step.getErrorMessage(),
                    step.getDurationMs(),
                    step.getCreatedAt(),
                    step.getCompletedAt()
            );
        }
    }

    public record EventResponse(
            UUID id,
            String eventType,
            String detail,
            Instant occurredAt
    ) {
        public static EventResponse from(com.nexaforge.workflow.domain.model.WorkflowEvent event) {
            return new EventResponse(
                    event.getId(),
                    event.getEventType(),
                    event.getDetail(),
                    event.getOccurredAt()
            );
        }
    }
}
