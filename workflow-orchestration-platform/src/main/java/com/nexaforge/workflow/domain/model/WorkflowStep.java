package com.nexaforge.workflow.domain.model;

import com.nexaforge.workflow.domain.enums.StepStatus;
import com.nexaforge.workflow.domain.enums.StepType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single step within a workflow execution.
 * Each step tracks its own status, retry count, and timing independently.
 */
@Entity
@Table(name = "workflow_steps", indexes = {
        @Index(name = "idx_step_workflow_id", columnList = "workflow_instance_id"),
        @Index(name = "idx_step_status", columnList = "status")
})
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstance workflowInstance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StepType stepType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StepStatus status;

    @Column(nullable = false)
    private int stepOrder;

    @Column(nullable = false)
    private int retryCount;

    private int maxRetries;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(length = 1024)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant startedAt;

    private Instant completedAt;

    /** Duration in milliseconds for execution metrics. */
    private Long durationMs;

    protected WorkflowStep() {
        // JPA
    }

    public static WorkflowStep create(StepType stepType, String input, int maxRetries) {
        WorkflowStep step = new WorkflowStep();
        step.stepType = stepType;
        step.status = StepStatus.PENDING;
        step.retryCount = 0;
        step.maxRetries = maxRetries;
        step.input = input;
        step.createdAt = Instant.now();
        return step;
    }

    // ───── State transitions ─────

    public void markInProgress() {
        this.status = StepStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public void markCompleted(String output) {
        this.status = StepStatus.COMPLETED;
        this.output = output;
        this.completedAt = Instant.now();
        if (startedAt != null) {
            this.durationMs = completedAt.toEpochMilli() - startedAt.toEpochMilli();
        }
    }

    public void markFailed(String errorMessage) {
        this.status = StepStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        if (startedAt != null) {
            this.durationMs = completedAt.toEpochMilli() - startedAt.toEpochMilli();
        }
    }

    public void markCompensating() {
        this.status = StepStatus.COMPENSATING;
    }

    public void markCompensated() {
        this.status = StepStatus.COMPENSATED;
        this.completedAt = Instant.now();
    }

    public void markSkipped() {
        this.status = StepStatus.SKIPPED;
        this.completedAt = Instant.now();
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    // ───── Getters / Setters ─────

    public UUID getId() { return id; }
    public WorkflowInstance getWorkflowInstance() { return workflowInstance; }
    public StepType getStepType() { return stepType; }
    public StepStatus getStatus() { return status; }
    public int getStepOrder() { return stepOrder; }
    public int getRetryCount() { return retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public String getInput() { return input; }
    public String getOutput() { return output; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getDurationMs() { return durationMs; }

    public void setWorkflowInstance(WorkflowInstance workflowInstance) {
        this.workflowInstance = workflowInstance;
    }

    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }
}
