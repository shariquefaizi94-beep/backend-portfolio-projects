package com.nexaforge.workflow.service.engine;

import com.nexaforge.workflow.domain.enums.StepType;
import com.nexaforge.workflow.domain.enums.WorkflowStatus;
import com.nexaforge.workflow.domain.event.WorkflowCommandEvent;
import com.nexaforge.workflow.domain.event.WorkflowStateEvent;
import com.nexaforge.workflow.domain.model.WorkflowInstance;
import com.nexaforge.workflow.domain.model.WorkflowStep;
import com.nexaforge.workflow.exception.WorkflowNotFoundException;
import com.nexaforge.workflow.kafka.producer.WorkflowEventProducer;
import com.nexaforge.workflow.repository.WorkflowInstanceRepository;
import com.nexaforge.workflow.service.IdempotencyService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Core workflow execution engine.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Executes workflow steps sequentially</li>
 *   <li>Handles retries with exponential backoff</li>
 *   <li>Triggers compensation on failure</li>
 *   <li>Publishes state change events to Kafka</li>
 *   <li>Enforces idempotency via Redis</li>
 *   <li>Uses optimistic locking for concurrency safety</li>
 * </ul>
 */
@Service
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private final WorkflowInstanceRepository repository;
    private final WorkflowEventProducer eventProducer;
    private final IdempotencyService idempotencyService;
    private final Map<StepType, StepExecutor> executors;

    // Metrics
    private final Counter stepsCompletedCounter;
    private final Counter stepsFailedCounter;
    private final Counter compensationsTriggeredCounter;
    private final Timer stepExecutionTimer;

    public WorkflowEngine(
            WorkflowInstanceRepository repository,
            WorkflowEventProducer eventProducer,
            IdempotencyService idempotencyService,
            List<StepExecutor> executorList,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.eventProducer = eventProducer;
        this.idempotencyService = idempotencyService;
        this.executors = executorList.stream()
                .collect(Collectors.toMap(StepExecutor::getStepType, Function.identity()));

        // Register metrics
        this.stepsCompletedCounter = Counter.builder("workflow.steps.completed")
                .description("Number of workflow steps completed successfully")
                .register(meterRegistry);
        this.stepsFailedCounter = Counter.builder("workflow.steps.failed")
                .description("Number of workflow steps that failed")
                .register(meterRegistry);
        this.compensationsTriggeredCounter = Counter.builder("workflow.compensations.triggered")
                .description("Number of compensations triggered")
                .register(meterRegistry);
        this.stepExecutionTimer = Timer.builder("workflow.step.execution.duration")
                .description("Duration of step execution")
                .register(meterRegistry);
    }

    /**
     * Starts workflow execution from the current step.
     * Called when the workflow is first created or when resuming after a transient failure.
     */
    @Transactional
    public void startExecution(UUID workflowId) {
        WorkflowInstance workflow = repository.findByIdWithSteps(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

        if (workflow.getStatus().isTerminal()) {
            log.info("Workflow {} is already in terminal state: {}", workflowId, workflow.getStatus());
            return;
        }

        String previousStatus = workflow.getStatus().name();
        workflow.transitionTo(WorkflowStatus.IN_PROGRESS);
        workflow.recordEvent("WORKFLOW_STARTED", "Workflow execution started");
        repository.save(workflow);

        publishStateEvent(workflow, previousStatus);
        executeNextStep(workflow);
    }

    /**
     * Executes the next pending step in the workflow.
     * On success, advances to the next step. On failure, triggers retry or compensation.
     */
    @Transactional
    public void executeNextStep(WorkflowInstance workflow) {
        if (!workflow.hasMoreSteps()) {
            completeWorkflow(workflow);
            return;
        }

        WorkflowStep step = workflow.getCurrentStep();
        if (step == null) {
            completeWorkflow(workflow);
            return;
        }

        StepExecutor executor = executors.get(step.getStepType());
        if (executor == null) {
            log.error("No executor registered for step type: {}", step.getStepType());
            handleStepFailure(workflow, step,
                    "No executor for step type: " + step.getStepType(), false);
            return;
        }

        // Idempotency check — skip if already processed
        String idempotencyKey = "%s:%s:%d".formatted(
                workflow.getId(), step.getStepType(), step.getRetryCount());
        if (!idempotencyService.tryAcquire(idempotencyKey)) {
            log.info("Step already processed (idempotent skip): {}", idempotencyKey);
            return;
        }

        step.markInProgress();
        workflow.recordEvent("STEP_STARTED",
                "Step %s started (attempt %d)".formatted(step.getStepType(), step.getRetryCount() + 1));

        try {
            String result = stepExecutionTimer.record(() -> executor.execute(workflow.getPayload()));
            handleStepSuccess(workflow, step, result);
        } catch (StepExecutionException e) {
            idempotencyService.release(idempotencyKey);
            handleStepFailure(workflow, step, e.getMessage(), e.isRetryable());
        } catch (Exception e) {
            idempotencyService.release(idempotencyKey);
            handleStepFailure(workflow, step, e.getMessage(), true);
        }
    }

    /**
     * Processes a command event received from Kafka.
     * This is the primary entry point for asynchronous step execution.
     */
    @Transactional
    public void processCommand(WorkflowCommandEvent command) {
        try {
            WorkflowInstance workflow = repository.findByIdWithSteps(command.workflowId())
                    .orElseThrow(() -> new WorkflowNotFoundException(command.workflowId()));

            switch (command.commandType()) {
                case "EXECUTE_STEP" -> executeNextStep(workflow);
                case "COMPENSATE" -> triggerCompensation(workflow.getId());
                case "RETRY_STEP" -> retryCurrentStep(workflow);
                default -> log.warn("Unknown command type: {}", command.commandType());
            }
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock conflict for workflow {}, will retry via Kafka",
                    command.workflowId());
            // The message will be retried by Kafka consumer retry mechanism
            throw e;
        }
    }

    /**
     * Triggers compensation (rollback) for all completed steps in reverse order.
     * Implements the Saga compensation pattern.
     */
    @Transactional
    public void triggerCompensation(UUID workflowId) {
        WorkflowInstance workflow = repository.findByIdWithSteps(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

        String previousStatus = workflow.getStatus().name();
        workflow.transitionTo(WorkflowStatus.COMPENSATING);
        workflow.recordEvent("COMPENSATION_STARTED", "Compensation triggered");
        compensationsTriggeredCounter.increment();

        List<WorkflowStep> completedSteps = workflow.getSteps().stream()
                .filter(s -> s.getStatus().requiresCompensation())
                .toList()
                .reversed();

        for (WorkflowStep step : completedSteps) {
            StepExecutor executor = executors.get(step.getStepType());
            if (executor != null) {
                try {
                    step.markCompensating();
                    executor.compensate(workflow.getPayload(), step.getOutput());
                    step.markCompensated();
                    workflow.recordEvent("STEP_COMPENSATED",
                            "Step %s compensated".formatted(step.getStepType()));
                } catch (Exception e) {
                    log.error("Compensation failed for step {} in workflow {}",
                            step.getStepType(), workflowId, e);
                    workflow.recordEvent("COMPENSATION_FAILED",
                            "Step %s compensation failed: %s".formatted(step.getStepType(), e.getMessage()));
                }
            }
        }

        workflow.transitionTo(WorkflowStatus.COMPENSATED);
        workflow.recordEvent("COMPENSATION_COMPLETED", "All steps compensated");
        repository.save(workflow);

        publishStateEvent(workflow, previousStatus);
    }

    // ───── Private helpers ─────

    private void handleStepSuccess(WorkflowInstance workflow, WorkflowStep step, String result) {
        step.markCompleted(result);
        stepsCompletedCounter.increment();
        workflow.recordEvent("STEP_COMPLETED",
                "Step %s completed successfully".formatted(step.getStepType()));

        workflow.advanceStep();

        if (workflow.hasMoreSteps()) {
            repository.save(workflow);
            // Publish command for next step execution (async via Kafka)
            eventProducer.sendCommand(WorkflowCommandEvent.create(
                    workflow.getId(),
                    workflow.getCorrelationId(),
                    "EXECUTE_STEP",
                    workflow.getCurrentStep().getStepType().name(),
                    workflow.getCurrentStepIndex(),
                    workflow.getPayload(),
                    0
            ));
        } else {
            completeWorkflow(workflow);
        }
    }

    private void handleStepFailure(WorkflowInstance workflow, WorkflowStep step,
                                   String errorMessage, boolean retryable) {
        step.markFailed(errorMessage);
        stepsFailedCounter.increment();
        workflow.recordEvent("STEP_FAILED",
                "Step %s failed: %s".formatted(step.getStepType(), errorMessage));

        if (retryable && step.canRetry()) {
            step.incrementRetry();
            workflow.incrementRetry();
            repository.save(workflow);

            log.info("Scheduling retry {} for step {} in workflow {}",
                    step.getRetryCount(), step.getStepType(), workflow.getId());

            eventProducer.sendCommand(WorkflowCommandEvent.create(
                    workflow.getId(),
                    workflow.getCorrelationId(),
                    "RETRY_STEP",
                    step.getStepType().name(),
                    workflow.getCurrentStepIndex(),
                    workflow.getPayload(),
                    step.getRetryCount()
            ));
        } else {
            String previousStatus = workflow.getStatus().name();
            workflow.fail("Step %s failed after %d attempts: %s"
                    .formatted(step.getStepType(), step.getRetryCount() + 1, errorMessage));
            repository.save(workflow);

            publishStateEvent(workflow, previousStatus);

            // Trigger compensation via Kafka for decoupling
            eventProducer.sendCommand(WorkflowCommandEvent.create(
                    workflow.getId(),
                    workflow.getCorrelationId(),
                    "COMPENSATE",
                    step.getStepType().name(),
                    workflow.getCurrentStepIndex(),
                    workflow.getPayload(),
                    0
            ));
        }
    }

    private void retryCurrentStep(WorkflowInstance workflow) {
        WorkflowStep step = workflow.getCurrentStep();
        if (step == null || !step.canRetry()) {
            log.warn("Cannot retry: no eligible step for workflow {}", workflow.getId());
            return;
        }
        step.markInProgress();
        workflow.recordEvent("STEP_RETRYING",
                "Retrying step %s (attempt %d)".formatted(step.getStepType(), step.getRetryCount() + 1));

        StepExecutor executor = executors.get(step.getStepType());
        if (executor == null) return;

        try {
            String result = stepExecutionTimer.record(() -> executor.execute(workflow.getPayload()));
            handleStepSuccess(workflow, step, result);
        } catch (StepExecutionException e) {
            handleStepFailure(workflow, step, e.getMessage(), e.isRetryable());
        }
    }

    private void completeWorkflow(WorkflowInstance workflow) {
        String previousStatus = workflow.getStatus().name();
        workflow.transitionTo(WorkflowStatus.COMPLETED);
        workflow.recordEvent("WORKFLOW_COMPLETED", "All steps completed successfully");
        repository.save(workflow);

        publishStateEvent(workflow, previousStatus);
        log.info("Workflow {} completed successfully", workflow.getId());
    }

    private void publishStateEvent(WorkflowInstance workflow, String previousStatus) {
        WorkflowStep currentStep = workflow.getCurrentStep();
        eventProducer.sendStateEvent(WorkflowStateEvent.create(
                workflow.getId(),
                workflow.getCorrelationId(),
                previousStatus,
                workflow.getStatus().name(),
                currentStep != null ? currentStep.getStepType().name() : "N/A",
                workflow.getCurrentStepIndex(),
                workflow.getStatus().getDescription()
        ));
    }
}
