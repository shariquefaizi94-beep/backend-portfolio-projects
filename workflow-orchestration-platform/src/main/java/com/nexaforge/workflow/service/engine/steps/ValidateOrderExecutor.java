package com.nexaforge.workflow.service.engine.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaforge.workflow.domain.enums.StepType;
import com.nexaforge.workflow.service.engine.StepExecutionException;
import com.nexaforge.workflow.service.engine.StepExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates order details — checks required fields, amounts, and business rules.
 * In a real system, this would validate against a product catalog and pricing engine.
 */
@Component
public class ValidateOrderExecutor implements StepExecutor {

    private static final Logger log = LoggerFactory.getLogger(ValidateOrderExecutor.class);
    private final ObjectMapper objectMapper;

    public ValidateOrderExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public StepType getStepType() {
        return StepType.VALIDATE_ORDER;
    }

    @Override
    public String execute(String payload) {
        log.info("Validating order: {}", payload);
        try {
            JsonNode order = objectMapper.readTree(payload);

            // Business validation rules
            if (!order.has("orderId") || order.get("orderId").asText().isBlank()) {
                throw new StepExecutionException("Order ID is required", false);
            }
            if (!order.has("customerId") || order.get("customerId").asText().isBlank()) {
                throw new StepExecutionException("Customer ID is required", false);
            }
            if (!order.has("totalAmount") || order.get("totalAmount").asDouble() <= 0) {
                throw new StepExecutionException("Total amount must be positive", false);
            }
            if (!order.has("items") || !order.get("items").isArray() || order.get("items").isEmpty()) {
                throw new StepExecutionException("Order must contain at least one item", false);
            }

            String result = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("validated", true)
                            .put("orderId", order.get("orderId").asText())
                            .put("totalAmount", order.get("totalAmount").asDouble())
                            .put("itemCount", order.get("items").size())
            );

            log.info("Order validation passed for order: {}", order.get("orderId").asText());
            return result;
        } catch (StepExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new StepExecutionException("Order validation failed: " + e.getMessage(), e, true);
        }
    }

    @Override
    public void compensate(String payload, String previousOutput) {
        log.info("Compensating order validation — no action needed (read-only step)");
        // Validation is read-only, no compensation needed
    }
}
