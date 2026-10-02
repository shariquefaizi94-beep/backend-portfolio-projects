package com.nexaforge.workflow.domain.model;

import com.nexaforge.workflow.domain.enums.WorkflowStatus;
import com.nexaforge.workflow.exception.InvalidStateTransitionException;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Root aggregate for a workflow execution.
 *
 * <p>Uses optimistic locking ({@code @Version}) to prevent concurrent
 * modification of the same workflow instance by multiple consumers.
 *
 * <p>The state machine transitions are enforced at the domain level
 * via {@link WorkflowStatus#canTransitionTo(WorkflowStatus)}.
 */
@Entity
@Table(name = "workflow_instances", indexes = {
        @Index(name = "idx_workflow_status", columnList = "status"),
        @Index(name = "idx_workflow_correlation_id", columnList = "correlationId", unique = true),
        @Index(name = "idx_workflow_created_at", columnList = "createdAt")
})
public class WorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * External correlation ID for idempotent workflow creation.
     * Callers use this to safely retry without creating duplicate workflows.
     */
    @Column(nullable = false, unique = true, length = 128)
    private String correlationId;

    @Column(nullable = false, length = 64)
    private String workflowType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkflowStatus status;

    /**
     * Optimistic lock version — prevents lost updates when multiple
     * Kafka consumers attempt to advance the same workflow concurrently.
     */
    @Version
    private Long version;

    @Column(nullable = false)
    private int currentStepIndex;

    @Column(nullable = false)
    private int retryCount;

    private int maxRetries;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(length = 1024)
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    private Instant completedAt;

    @OneToMany(mappedBy = "workflowInstance", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<WorkflowStep> steps = new ArrayList<>();

    @OneToMany(mappedBy = "workflowInstance", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt ASC")
    private List<WorkflowEvent> events = new ArrayList<>();

    protected WorkflowInstance() {
        // JPA
    }

    /**
     * Factory method — the only way to create a new workflow.
     */
    public static WorkflowInstance create(String correlationId,
                                          String workflowType,
                                          String payload,
                                          int maxRetries) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.correlationId = correlationId;
        instance.workflowType = workflowType;
        instance.status = WorkflowStatus.PENDING;
        instance.currentStepIndex = 0;
        instance.retryCount = 0;
        instance.maxRetries = maxRetries;
        instance.payload = payload;
        instance.createdAt = Instant.now();
        instance.updatedAt = Instant.now();
        return instance;
    }

    // ───── State transitions ─────

    public void transitionTo(WorkflowStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidStateTransitionException(
                    "Cannot transition workflow %s from %s to %s"
                            .formatted(id, status, newStatus));
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
        if (newStatus.isTerminal()) {
            this.completedAt = Instant.now();
        }
    }

    public void advanceStep() {
        this.currentStepIndex++;
        this.updatedAt = Instant.now();
    }

    public void incrementRetry() {
        this.retryCount++;
        this.updatedAt = Instant.now();
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public void fail(String reason) {
        this.failureReason = reason;
        transitionTo(WorkflowStatus.FAILED);
    }

    // ───── Step management ─────

    public void addStep(WorkflowStep step) {
        step.setWorkflowInstance(this);
        step.setStepOrder(steps.size());
        steps.add(step);
    }

    public WorkflowStep getCurrentStep() {
        if (currentStepIndex >= steps.size()) {
            return null;
        }
        return steps.get(currentStepIndex);
    }

    public boolean hasMoreSteps() {
        return currentStepIndex < steps.size();
    }

    // ───── Event recording ─────

    public void recordEvent(String eventType, String detail) {
        WorkflowEvent event = WorkflowEvent.create(this, eventType, detail);
        events.add(event);
    }

    // ───── Getters ─────

    public UUID getId() { return id; }
    public String getCorrelationId() { return correlationId; }
    public String getWorkflowType() { return workflowType; }
    public WorkflowStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public int getCurrentStepIndex() { return currentStepIndex; }
    public int getRetryCount() { return retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public String getPayload() { return payload; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public List<WorkflowStep> getSteps() { return List.copyOf(steps); }
    public List<WorkflowEvent> getEvents() { return List.copyOf(events); }
}
