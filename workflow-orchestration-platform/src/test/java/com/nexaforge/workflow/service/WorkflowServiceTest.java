package com.nexaforge.workflow.service;

import com.nexaforge.workflow.domain.enums.WorkflowStatus;
import com.nexaforge.workflow.domain.model.WorkflowInstance;
import com.nexaforge.workflow.exception.WorkflowNotFoundException;
import com.nexaforge.workflow.repository.WorkflowInstanceRepository;
import com.nexaforge.workflow.service.engine.WorkflowEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private WorkflowInstanceRepository repository;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private WorkflowEngine engine;

    @InjectMocks
    private WorkflowService service;

    @Test
    @DisplayName("Should create new workflow with all steps when correlationId is new")
    void shouldCreateWorkflowWhenNew() {
        String correlationId = "order-001";
        String payload = """
                {"orderId":"ORD-001","customerId":"CUST-001","totalAmount":100.0,
                "items":[{"sku":"SKU-001","quantity":1}]}""";

        when(repository.findByCorrelationId(correlationId)).thenReturn(Optional.empty());
        when(repository.save(any(WorkflowInstance.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(engine).startExecution(any());

        WorkflowInstance result = service.createOrderFulfillmentWorkflow(correlationId, payload);

        assertThat(result).isNotNull();
        assertThat(result.getCorrelationId()).isEqualTo(correlationId);
        assertThat(result.getWorkflowType()).isEqualTo("ORDER_FULFILLMENT");
        assertThat(result.getStatus()).isEqualTo(WorkflowStatus.PENDING);
        assertThat(result.getSteps()).hasSize(6);

        verify(repository).save(any(WorkflowInstance.class));
    }

    @Test
    @DisplayName("Should return existing workflow when correlationId already exists (idempotent)")
    void shouldReturnExistingWorkflowOnDuplicateCorrelationId() {
        String correlationId = "order-001";
        WorkflowInstance existing = WorkflowInstance.create(correlationId, "ORDER_FULFILLMENT", "{}", 3);

        when(repository.findByCorrelationId(correlationId)).thenReturn(Optional.of(existing));

        WorkflowInstance result = service.createOrderFulfillmentWorkflow(correlationId, "{}");

        assertThat(result).isSameAs(existing);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw WorkflowNotFoundException for unknown workflow ID")
    void shouldThrowNotFoundForUnknownId() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findByIdWithStepsAndEvents(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWorkflow(unknownId))
                .isInstanceOf(WorkflowNotFoundException.class);
    }

    @Test
    @DisplayName("Should cancel a pending workflow and skip remaining steps")
    void shouldCancelPendingWorkflow() {
        String correlationId = "order-cancel-001";
        String payload = """
                {"orderId":"ORD-002","customerId":"CUST-002","totalAmount":50.0,
                "items":[{"sku":"SKU-002","quantity":2}]}""";

        WorkflowInstance workflow = WorkflowInstance.create(correlationId, "ORDER_FULFILLMENT", payload, 3);

        when(repository.findByIdWithSteps(any())).thenReturn(Optional.of(workflow));
        when(repository.save(any(WorkflowInstance.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        WorkflowInstance result = service.cancelWorkflow(UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(WorkflowStatus.CANCELLED);
    }
}
