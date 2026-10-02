package com.nexaforge.workflow.service.engine.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaforge.workflow.domain.enums.StepType;
import com.nexaforge.workflow.service.engine.StepExecutionException;
import com.nexaforge.workflow.service.engine.StepExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Final workflow step — marks the order as fulfilled.
 * In production this would update the order management system.
 */
@Component
public class CompleteOrderExecutor implements StepExecutor {

    private static final Logger log = LoggerFactory.getLogger(CompleteOrderExecutor.class);
    private final ObjectMapper objectMapper;

    public CompleteOrderExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public StepType getStepType() {
        return StepType.COMPLETE_ORDER;
    }

    @Override
    public String execute(String payload) {
        log.info("Completing order fulfillment");
        try {
            JsonNode order = objectMapper.readTree(payload);

            String result = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("orderId", order.get("orderId").asText())
                            .put("status", "FULFILLED")
                            .put("completedAt", Instant.now().toString())
            );

            log.info("Order {} fulfillment completed", order.get("orderId").asText());
            return result;
        } catch (Exception e) {
            throw new StepExecutionException("Order completion failed: " + e.getMessage(), e, true);
        }
    }

    @Override
    public void compensate(String payload, String previousOutput) {
        log.info("Compensating order completion — reverting order status");
        try {
            if (previousOutput != null) {
                JsonNode result = objectMapper.readTree(previousOutput);
                log.info("Order {} status reverted from FULFILLED", result.get("orderId").asText());
            }
        } catch (Exception e) {
            log.error("Failed to compensate order completion", e);
        }
    }
}
