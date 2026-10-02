package com.nexaforge.order.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaforge.order.service.OrderService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Saga coordinator — listens for events from downstream services
 * and advances the order through its lifecycle.
 *
 * <p>Each handler is idempotent: processing the same event twice
 * will fail the state transition (already in the target state)
 * and the duplicate is safely ignored via optimistic locking.
 *
 * <p>Event flow:
 * <pre>
 *   PaymentCompleted  → mark order PAYMENT_COMPLETED
 *   PaymentFailed     → mark order FAILED → trigger compensation
 *   InventoryReserved → mark order INVENTORY_RESERVED
 *   InventoryFailed   → mark order FAILED → trigger compensation
 *   ShippingArranged  → mark order COMPLETED
 * </pre>
 */
@Component
public class SagaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SagaEventConsumer.class);

    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final Counter eventsProcessed;
    private final Counter eventsFailed;

    public SagaEventConsumer(OrderService orderService, ObjectMapper objectMapper,
                             MeterRegistry meterRegistry) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
        this.eventsProcessed = Counter.builder("saga.events.processed").register(meterRegistry);
        this.eventsFailed = Counter.builder("saga.events.failed").register(meterRegistry);
    }

    @KafkaListener(topics = "${kafka.topics.payment-events}", groupId = "order-saga")
    public void handlePaymentEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processEvent(record, ack, "payment");
    }

    @KafkaListener(topics = "${kafka.topics.inventory-events}", groupId = "order-saga")
    public void handleInventoryEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processEvent(record, ack, "inventory");
    }

    @KafkaListener(topics = "${kafka.topics.shipping-events}", groupId = "order-saga")
    public void handleShippingEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processEvent(record, ack, "shipping");
    }

    private void processEvent(ConsumerRecord<String, String> record, Acknowledgment ack, String source) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = determineEventType(event);
            String orderId = event.path("orderId").asText();

            MDC.put("correlationId", event.path("correlationId").asText());
            log.info("Saga received {} from {} for order {} [partition={}, offset={}]",
                    eventType, source, orderId, record.partition(), record.offset());

            switch (eventType) {
                case "PaymentCompleted" -> orderService.handlePaymentCompleted(
                        orderId, event.path("transactionId").asText());
                case "PaymentFailed" -> orderService.handlePaymentFailed(
                        orderId, event.path("reason").asText());
                case "InventoryReserved" -> orderService.handleInventoryReserved(
                        orderId, event.path("reservationId").asText());
                case "InventoryReservationFailed" -> orderService.handlePaymentFailed(
                        orderId, "Inventory reservation failed: " + event.path("reason").asText());
                case "ShippingArranged" -> orderService.handleShippingArranged(
                        orderId, event.path("shipmentId").asText(), event.path("trackingNumber").asText());
                default -> log.warn("Unknown event type from {}: {}", source, eventType);
            }

            eventsProcessed.increment();
            ack.acknowledge();
        } catch (Exception e) {
            eventsFailed.increment();
            log.error("Failed to process {} event", source, e);
            ack.acknowledge(); // Ack to prevent infinite loop; error is logged for investigation
        } finally {
            MDC.clear();
        }
    }

    private String determineEventType(JsonNode event) {
        if (event.has("transactionId") && event.has("amount")) return "PaymentCompleted";
        if (event.has("reservationId") && event.has("reservedItems")) return "InventoryReserved";
        if (event.has("shipmentId") && event.has("trackingNumber")) return "ShippingArranged";
        if (event.has("reason")) {
            if (event.has("transactionId")) return "PaymentFailed";
            return "InventoryReservationFailed";
        }
        return "Unknown";
    }
}
