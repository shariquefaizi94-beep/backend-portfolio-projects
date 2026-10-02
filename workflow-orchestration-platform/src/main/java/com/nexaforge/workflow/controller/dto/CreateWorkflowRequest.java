package com.nexaforge.workflow.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new workflow.
 *
 * @param correlationId unique identifier from the caller for idempotent creation
 * @param payload order details as JSON string
 */
public record CreateWorkflowRequest(

        @NotBlank(message = "Correlation ID is required")
        @Size(max = 128, message = "Correlation ID must not exceed 128 characters")
        String correlationId,

        @NotBlank(message = "Payload is required")
        String payload
) {}
