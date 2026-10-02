package com.nexaforge.workflow.service;

import com.nexaforge.workflow.domain.enums.StepType;
import com.nexaforge.workflow.domain.enums.WorkflowStatus;
import com.nexaforge.workflow.domain.model.WorkflowInstance;
import com.nexaforge.workflow.domain.model.WorkflowStep;
import com.nexaforge.workflow.exception.DuplicateWorkflowException;
import com.nexaforge.workflow.exception.WorkflowNotFoundException;
import com.nexaforge.workflow.repository.WorkflowInstanceRepository;
import com.nexaforge.workflow.service.engine.WorkflowEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for workflow lifecycle management.
 * Provides the API-facing operations: create, query, cancel.
 *
 * <p>Workflow creation is idempotent — submitting the same correlationId
 * twice returns the existing workflow rather than creating a duplicate.
 */
@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);
    private static final int DEFAULT_MAX_RETRIES = 3;

    private final WorkflowInstanceRepository repository;
    private final WorkflowEngine engine;

    public WorkflowService(WorkflowInstanceRepository repository, WorkflowEngine engine) {
        this.repository = repository;
        this.engine = engine;
    }

    /**
     * Creates a new order fulfillment workflow with the standard step sequence.
     * Idempotent: returns existing workflow if correlationId already exists.
     */
    @Transactional
    public WorkflowInstance createOrderFulfillmentWorkflow(String correlationId, String payload) {
        // Idempotency check — return existing workflow if already created
        return repository.findByCorrelationId(correlationId)
                .map(existing -> {
                    log.info("Returning existing workflow for correlationId: {}", correlationId);
                    return existing;
                })
                .orElseGet(() -> {
                    WorkflowInstance workflow = WorkflowInstance.create(
                            correlationId,
                            "ORDER_FULFILLMENT",
                            payload,
                            DEFAULT_MAX_RETRIES
                    );

                    // Build the step pipeline
                    workflow.addStep(WorkflowStep.create(StepType.VALIDATE_ORDER, payload, DEFAULT_MAX_RETRIES));
                    workflow.addStep(WorkflowStep.create(StepType.PROCESS_PAYMENT, payload, DEFAULT_MAX_RETRIES));
                    workflow.addStep(WorkflowStep.create(StepType.RESERVE_INVENTORY, payload, DEFAULT_MAX_RETRIES));
                    workflow.addStep(WorkflowStep.create(StepType.ARRANGE_SHIPPING, payload, DEFAULT_MAX_RETRIES));
                    workflow.addStep(WorkflowStep.create(StepType.SEND_NOTIFICATION, payload, DEFAULT_MAX_RETRIES));
                    workflow.addStep(WorkflowStep.create(StepType.COMPLETE_ORDER, payload, DEFAULT_MAX_RETRIES));

                    workflow.recordEvent("WORKFLOW_CREATED",
                            "Order fulfillment workflow created with %d steps"
                                    .formatted(workflow.getSteps().size()));

                    WorkflowInstance saved = repository.save(workflow);
                    log.info("Created workflow {} for correlationId: {}", saved.getId(), correlationId);

                    // Start execution asynchronously
                    engine.startExecution(saved.getId());

                    return saved;
                });
    }

    @Transactional(readOnly = true)
    public WorkflowInstance getWorkflow(UUID workflowId) {
        return repository.findByIdWithStepsAndEvents(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
    }

    @Transactional(readOnly = true)
    public WorkflowInstance getWorkflowByCorrelationId(String correlationId) {
        return repository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new WorkflowNotFoundException(correlationId));
    }

    @Transactional(readOnly = true)
    public Page<WorkflowInstance> listWorkflows(WorkflowStatus status, Pageable pageable) {
        if (status != null) {
            return repository.findByStatus(status, pageable);
        }
        return repository.findAll(pageable);
    }

    /**
     * Cancels a workflow that has not yet completed.
     */
    @Transactional
    public WorkflowInstance cancelWorkflow(UUID workflowId) {
        WorkflowInstance workflow = repository.findByIdWithSteps(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

        if (workflow.getStatus().isTerminal()) {
            throw new IllegalStateException(
                    "Cannot cancel workflow in terminal state: " + workflow.getStatus());
        }

        workflow.transitionTo(WorkflowStatus.CANCELLED);
        workflow.recordEvent("WORKFLOW_CANCELLED", "Workflow cancelled by user");

        // Mark remaining pending steps as skipped
        workflow.getSteps().stream()
                .filter(s -> s.getStatus() == com.nexaforge.workflow.domain.enums.StepStatus.PENDING)
                .forEach(WorkflowStep::markSkipped);

        return repository.save(workflow);
    }

    @Transactional(readOnly = true)
    public WorkflowStats getStats() {
        return new WorkflowStats(
                repository.count(),
                repository.countByStatus(WorkflowStatus.PENDING),
                repository.countByStatus(WorkflowStatus.IN_PROGRESS),
                repository.countByStatus(WorkflowStatus.COMPLETED),
                repository.countByStatus(WorkflowStatus.FAILED),
                repository.countByStatus(WorkflowStatus.COMPENSATED)
        );
    }

    public record WorkflowStats(
            long total,
            long pending,
            long inProgress,
            long completed,
            long failed,
            long compensated
    ) {}
}
