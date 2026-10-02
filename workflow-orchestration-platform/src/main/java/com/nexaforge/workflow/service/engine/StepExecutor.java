package com.nexaforge.workflow.service.engine;

import com.nexaforge.workflow.domain.enums.StepType;

/**
 * Contract for individual workflow step executors.
 * Each implementation handles one step type and provides
 * both execute (forward) and compensate (rollback) logic.
 */
public interface StepExecutor {

    /**
     * The step type this executor handles.
     */
    StepType getStepType();

    /**
     * Execute the step's business logic.
     *
     * @param payload the workflow payload (JSON)
     * @return result of execution (JSON)
     * @throws StepExecutionException if the step fails
     */
    String execute(String payload);

    /**
     * Compensate (undo) the step's effects.
     * Called during workflow rollback when a downstream step fails.
     *
     * @param payload the workflow payload (JSON)
     * @param previousOutput the output from the original execution
     */
    void compensate(String payload, String previousOutput);
}
