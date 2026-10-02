package com.nexaforge.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Simulates sending notifications via email/SMS.
 * In production, this would integrate with SendGrid, Twilio, etc.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final ObjectMapper objectMapper;
    private final Counter notificationsSent;

    public NotificationService(ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.notificationsSent = Counter.builder("notifications.sent").register(meterRegistry);
    }

    public void processNotification(String eventJson) {
        try {
            JsonNode event = objectMapper.readTree(eventJson);
            String orderId = event.path("orderId").asText("unknown");
            String notificationType = determineType(event);

            log.info("📧 Sending {} notification for order {}", notificationType, orderId);
            // Simulate sending email/SMS — in production: call external API
            notificationsSent.increment();
            log.info("✅ Notification sent: {} for order {}", notificationType, orderId);
        } catch (Exception e) {
            log.error("Failed to process notification event", e);
        }
    }

    private String determineType(JsonNode event) {
        if (event.has("transactionId") && event.has("amount")) return "PAYMENT_CONFIRMED";
        if (event.has("trackingNumber")) return "SHIPMENT_CREATED";
        if (event.has("totalAmount") && event.has("items")) return "ORDER_PLACED";
        if (event.has("reason")) return "ORDER_ISSUE";
        return "GENERAL";
    }
}
