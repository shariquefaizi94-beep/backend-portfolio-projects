package com.nexaforge.inventory.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaforge.inventory.domain.model.InventoryReservation;
import com.nexaforge.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Listens for PaymentCompleted events to reserve inventory.
 * Publishes InventoryReserved or InventoryReservationFailed.
 * Also handles compensation (release) events.
 */
@Component
public class InventoryCommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryCommandConsumer.class);

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.inventory-events}")
    private String inventoryEventsTopic;

    public InventoryCommandConsumer(InventoryService inventoryService,
                                    KafkaTemplate<String, Object> kafkaTemplate,
                                    ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topics.payment-events}", groupId = "inventory-service")
    public void handlePaymentCompleted(String record, Acknowledgment ack) {
        try {
            JsonNode event = objectMapper.readTree(record);
            if (!event.has("transactionId") || event.has("reason")) {
                ack.acknowledge();
                return; // Not a PaymentCompleted event
            }

            String orderId = event.path("orderId").asText();
            MDC.put("correlationId", event.path("correlationId").asText());
            log.info("Reserving inventory for order {}", orderId);

            InventoryReservation reservation = inventoryService.reserveInventory(orderId, "{}");

            var resultEvent = objectMapper.createObjectNode()
                    .put("orderId", orderId)
                    .put("reservationId", reservation.getReservationId())
                    .put("correlationId", orderId);
            resultEvent.putArray("reservedItems");
            kafkaTemplate.send(inventoryEventsTopic, orderId, resultEvent.toString());

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process inventory reservation", e);
            ack.acknowledge();
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(topics = "${kafka.topics.compensation-events}", groupId = "inventory-compensation")
    public void handleCompensation(String record, Acknowledgment ack) {
        try {
            JsonNode event = objectMapper.readTree(record);
            String orderId = event.path("orderId").asText();
            log.info("Releasing inventory reservation for order {}", orderId);
            inventoryService.releaseReservation(orderId);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process inventory compensation", e);
            ack.acknowledge();
        }
    }
}
