package com.portfolio.cloudnative.notification.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Notification domain model for the notification microservice.
 */
public record Notification(
    UUID id,
    UUID userId,
    NotificationType type,
    NotificationChannel channel,
    String subject,
    String message,
    Map<String, String> metadata,
    NotificationStatus status,
    Instant scheduledAt,
    Instant sentAt,
    Instant createdAt
) {
    
    public Notification {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Notification type cannot be null");
        }
        if (channel == null) {
            throw new IllegalArgumentException("Notification channel cannot be null");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or blank");
        }
    }
    
    public static Notification create(UUID userId, NotificationType type, NotificationChannel channel,
                                       String subject, String message, Map<String, String> metadata) {
        return new Notification(
            UUID.randomUUID(),
            userId,
            type,
            channel,
            subject,
            message,
            metadata != null ? Map.copyOf(metadata) : Map.of(),
            NotificationStatus.PENDING,
            null,
            null,
            Instant.now()
        );
    }
    
    public static Notification scheduled(UUID userId, NotificationType type, NotificationChannel channel,
                                          String subject, String message, Instant scheduledAt) {
        return new Notification(
            UUID.randomUUID(),
            userId,
            type,
            channel,
            subject,
            message,
            Map.of(),
            NotificationStatus.SCHEDULED,
            scheduledAt,
            null,
            Instant.now()
        );
    }
    
    public Notification markSent() {
        return new Notification(id, userId, type, channel, subject, message, metadata,
            NotificationStatus.SENT, scheduledAt, Instant.now(), createdAt);
    }
    
    public Notification markFailed() {
        return new Notification(id, userId, type, channel, subject, message, metadata,
            NotificationStatus.FAILED, scheduledAt, null, createdAt);
    }
}
