package com.nexaforge.workflow.service;

import com.nexaforge.workflow.domain.enums.WorkflowStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the state machine invariants of WorkflowStatus.
 * Validates that only allowed transitions are permitted.
 */
class WorkflowStatusTest {

    @Test
    @DisplayName("PENDING can transition to IN_PROGRESS or CANCELLED only")
    void pendingTransitions() {
        assertThat(WorkflowStatus.PENDING.canTransitionTo(WorkflowStatus.IN_PROGRESS)).isTrue();
        assertThat(WorkflowStatus.PENDING.canTransitionTo(WorkflowStatus.CANCELLED)).isTrue();
        assertThat(WorkflowStatus.PENDING.canTransitionTo(WorkflowStatus.COMPLETED)).isFalse();
        assertThat(WorkflowStatus.PENDING.canTransitionTo(WorkflowStatus.FAILED)).isFalse();
    }

    @Test
    @DisplayName("IN_PROGRESS can transition to COMPLETED, FAILED, or CANCELLED")
    void inProgressTransitions() {
        assertThat(WorkflowStatus.IN_PROGRESS.canTransitionTo(WorkflowStatus.COMPLETED)).isTrue();
        assertThat(WorkflowStatus.IN_PROGRESS.canTransitionTo(WorkflowStatus.FAILED)).isTrue();
        assertThat(WorkflowStatus.IN_PROGRESS.canTransitionTo(WorkflowStatus.CANCELLED)).isTrue();
        assertThat(WorkflowStatus.IN_PROGRESS.canTransitionTo(WorkflowStatus.PENDING)).isFalse();
    }

    @Test
    @DisplayName("FAILED can transition to COMPENSATING or CANCELLED")
    void failedTransitions() {
        assertThat(WorkflowStatus.FAILED.canTransitionTo(WorkflowStatus.COMPENSATING)).isTrue();
        assertThat(WorkflowStatus.FAILED.canTransitionTo(WorkflowStatus.CANCELLED)).isTrue();
        assertThat(WorkflowStatus.FAILED.canTransitionTo(WorkflowStatus.COMPLETED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = WorkflowStatus.class, names = {"COMPLETED", "COMPENSATED", "CANCELLED"})
    @DisplayName("Terminal states have no valid transitions")
    void terminalStatesHaveNoTransitions(WorkflowStatus terminalState) {
        assertThat(terminalState.isTerminal()).isTrue();
        assertThat(terminalState.validTransitions()).isEmpty();
    }
}
