package com.nexaforge.workflow.service.engine.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaforge.workflow.domain.enums.StepType;
import com.nexaforge.workflow.service.engine.StepExecutionException;
import com.nexaforge.workflow.service.engine.StepExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simulates shipping arrangement — creates shipment label and tracking.
 * Compensation cancels the shipment.
 */
@Component
public class ArrangeShippingExecutor implements StepExecutor {

    private static final Logger log = LoggerFactory.getLogger(ArrangeShippingExecutor.class);
    private final ObjectMapper objectMapper;

    public ArrangeShippingExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public StepType getStepType() {
        return StepType.ARRANGE_SHIPPING;
    }

    @Override
    public String execute(String payload) {
        log.info("Arranging shipping for order");
        try {
            JsonNode order = objectMapper.readTree(payload);

            String shipmentId = "SHP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String trackingNumber = "TRK" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 12).toUpperCase();

            String result = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("shipmentId", shipmentId)
                            .put("trackingNumber", trackingNumber)
                            .put("carrier", "NexaShip Express")
                            .put("estimatedDeliveryDays", 3)
                            .put("status", "LABEL_CREATED")
            );

            log.info("Shipping arranged: {} tracking: {}", shipmentId, trackingNumber);
            return result;
        } catch (Exception e) {
            throw new StepExecutionException("Shipping arrangement failed: " + e.getMessage(), e, true);
        }
    }

    @Override
    public void compensate(String payload, String previousOutput) {
        log.info("Compensating shipping — cancelling shipment");
        try {
            if (previousOutput != null) {
                JsonNode shippingResult = objectMapper.readTree(previousOutput);
                String shipmentId = shippingResult.get("shipmentId").asText();
                log.info("Shipment cancelled: {}", shipmentId);
            }
        } catch (Exception e) {
            log.error("Failed to compensate shipping", e);
        }
    }
}
