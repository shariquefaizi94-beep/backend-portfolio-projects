package com.nexaforge.shipping.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaforge.shipping.domain.model.Shipment;
import com.nexaforge.shipping.service.ShippingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ShippingCommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(ShippingCommandConsumer.class);

    private final ShippingService shippingService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.shipping-events}")
    private String shippingEventsTopic;

    public ShippingCommandConsumer(ShippingService shippingService,
                                   KafkaTemplate<String, Object> kafkaTemplate,
                                   ObjectMapper objectMapper) {
        this.shippingService = shippingService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topics.inventory-events}", groupId = "shipping-service")
    public void handleInventoryReserved(String record, Acknowledgment ack) {
        try {
            JsonNode event = objectMapper.readTree(record);
            if (!event.has("reservationId")) {
                ack.acknowledge();
                return;
            }

            String orderId = event.path("orderId").asText();
            log.info("Arranging shipping for order {}", orderId);

            Shipment shipment = shippingService.arrangeShipping(orderId);

            var resultEvent = objectMapper.createObjectNode()
                    .put("orderId", orderId)
                    .put("shipmentId", shipment.getShipmentId())
                    .put("trackingNumber", shipment.getTrackingNumber())
                    .put("carrier", shipment.getCarrier())
                    .put("estimatedDays", 3)
                    .put("correlationId", orderId);
            kafkaTemplate.send(shippingEventsTopic, orderId, resultEvent.toString());

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to arrange shipping", e);
            ack.acknowledge();
        }
    }
}
