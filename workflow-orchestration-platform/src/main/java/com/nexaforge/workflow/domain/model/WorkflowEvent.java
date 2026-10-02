package com.nexaforge.workflow.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit trail entry for a workflow execution.
 * Every state transition and significant action is recorded as an event.
 * This provides a complete history for debugging, auditing, and replay.
 */
@Entity
@Table(name = "workflow_events", indexes = {
        @Index(name = "idx_event_workflow_id", columnList = "workflow_instance_id"),
        @Index(name = "idx_event_occurred_at", columnList = "occurredAt"),
        @Index(name = "idx_event_type", columnList = "eventType")
})
public class WorkflowEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstance workflowInstance;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    protected WorkflowEvent() {
        // JPA
    }

    public static WorkflowEvent create(WorkflowInstance workflowInstance,
                                       String eventType,
                                       String detail) {
        WorkflowEvent event = new WorkflowEvent();
        event.workflowInstance = workflowInstance;
        event.eventType = eventType;
        event.detail = detail;
        event.occurredAt = Instant.now();
        return event;
    }

    public UUID getId() { return id; }
    public WorkflowInstance getWorkflowInstance() { return workflowInstance; }
    public String getEventType() { return eventType; }
    public String getDetail() { return detail; }
    public Instant getOccurredAt() { return occurredAt; }
}
