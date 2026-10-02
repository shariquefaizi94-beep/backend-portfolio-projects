package com.nexaforge.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaforge.workflow.controller.dto.CreateWorkflowRequest;
import com.nexaforge.workflow.domain.model.WorkflowInstance;
import com.nexaforge.workflow.exception.WorkflowNotFoundException;
import com.nexaforge.workflow.service.WorkflowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkflowController.class)
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkflowService workflowService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public WorkflowService workflowService() {
            return mock(WorkflowService.class);
        }
    }

    private static final String PAYLOAD = """
            {"orderId":"ORD-001","customerId":"CUST-001","totalAmount":100.0,
            "items":[{"sku":"SKU-001","quantity":1}]}""";

    @Test
    @DisplayName("POST /api/v1/workflows should create and return workflow")
    void shouldCreateWorkflow() throws Exception {
        WorkflowInstance workflow = WorkflowInstance.create("order-001", "ORDER_FULFILLMENT", PAYLOAD, 3);
        when(workflowService.createOrderFulfillmentWorkflow(anyString(), anyString()))
                .thenReturn(workflow);

        CreateWorkflowRequest request = new CreateWorkflowRequest("order-001", PAYLOAD);

        mockMvc.perform(post("/api/v1/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correlationId").value("order-001"))
                .andExpect(jsonPath("$.workflowType").value("ORDER_FULFILLMENT"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/workflows should fail validation with blank correlationId")
    void shouldRejectBlankCorrelationId() throws Exception {
        CreateWorkflowRequest request = new CreateWorkflowRequest("", PAYLOAD);

        mockMvc.perform(post("/api/v1/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/workflows/{id} should return 404 for unknown ID")
    void shouldReturn404ForUnknownWorkflow() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(workflowService.getWorkflow(unknownId))
                .thenThrow(new WorkflowNotFoundException(unknownId));

        mockMvc.perform(get("/api/v1/workflows/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Workflow Not Found"));
    }
}
