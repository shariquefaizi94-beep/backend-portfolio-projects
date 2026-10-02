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
 * Simulates sending order confirmation notifications.
 * No compensation needed — notifications are fire-and-forget in this model.
 */
@Component
public class SendNotificationExecutor implements StepExecutor {

    private static final Logger log = LoggerFactory.getLogger(SendNotificationExecutor.class);
    private final ObjectMapper objectMapper;

    public SendNotificationExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public StepType getStepType() {
        return StepType.SEND_NOTIFICATION;
    }

    @Override
    public String execute(String payload) {
        log.info("Sending order notification");
        try {
            JsonNode order = objectMapper.readTree(payload);

            String result = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("notificationType", "ORDER_CONFIRMATION")
                            .put("channel", "EMAIL")
                            .put("recipient", order.has("customerEmail")
                                    ? order.get("customerEmail").asText()
                                    : "customer@example.com")
                            .put("sentAt", Instant.now().toString())
                            .put("status", "SENT")
            );

            log.info("Notification sent for order: {}", order.get("orderId").asText());
            return result;
        } catch (Exception e) {
            throw new StepExecutionException("Notification failed: " + e.getMessage(), e, true);
        }
    }

    @Override
    public void compensate(String payload, String previousOutput) {
        log.info("Notification compensation — no rollback for sent notifications");
    }
}
