package com.nexaforge.workflow.service.engine.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexaforge.workflow.domain.enums.StepType;
import com.nexaforge.workflow.service.engine.StepExecutionException;
import com.nexaforge.workflow.service.engine.StepExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simulates inventory reservation for order items.
 * In production this would interact with an inventory management system.
 *
 * <p>Compensation releases the reserved inventory back to available stock.
 */
@Component
public class ReserveInventoryExecutor implements StepExecutor {

    private static final Logger log = LoggerFactory.getLogger(ReserveInventoryExecutor.class);
    private final ObjectMapper objectMapper;

    public ReserveInventoryExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public StepType getStepType() {
        return StepType.RESERVE_INVENTORY;
    }

    @Override
    public String execute(String payload) {
        log.info("Reserving inventory for order");
        try {
            JsonNode order = objectMapper.readTree(payload);
            JsonNode items = order.get("items");

            String reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            ObjectNode result = objectMapper.createObjectNode();
            result.put("reservationId", reservationId);

            ArrayNode reservedItems = result.putArray("reservedItems");
            for (JsonNode item : items) {
                ObjectNode reservedItem = objectMapper.createObjectNode();
                reservedItem.put("sku", item.get("sku").asText());
                reservedItem.put("quantity", item.get("quantity").asInt());
                reservedItem.put("warehouseId", "WH-EAST-01");
                reservedItems.add(reservedItem);
            }

            result.put("status", "RESERVED");

            log.info("Inventory reserved: {} ({} items)", reservationId, items.size());
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new StepExecutionException("Inventory reservation failed: " + e.getMessage(), e, true);
        }
    }

    @Override
    public void compensate(String payload, String previousOutput) {
        log.info("Compensating inventory — releasing reservation");
        try {
            if (previousOutput != null) {
                JsonNode reservation = objectMapper.readTree(previousOutput);
                String reservationId = reservation.get("reservationId").asText();
                // In production: release inventory reservation
                log.info("Inventory reservation released: {}", reservationId);
            }
        } catch (Exception e) {
            log.error("Failed to compensate inventory reservation", e);
        }
    }
}
