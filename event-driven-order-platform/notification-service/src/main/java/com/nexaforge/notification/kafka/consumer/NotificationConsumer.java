package com.nexaforge.notification.kafka.consumer;

import com.nexaforge.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Listens for events across all services and sends notifications.
 * This is a "listen-only" participant — does not affect the saga.
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final NotificationService notificationService;

    public NotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = {"${kafka.topics.order-events}", "${kafka.topics.payment-events}",
            "${kafka.topics.shipping-events}"}, groupId = "notification-service")
    public void handleEvent(String record, Acknowledgment ack) {
        try {
            notificationService.processNotification(record);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process notification", e);
            ack.acknowledge();
        }
    }
}
