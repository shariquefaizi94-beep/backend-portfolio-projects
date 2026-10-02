package com.portfolio.cloudnative.notification.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Notification Domain Tests")
class NotificationTest {
    
    private static final UUID USER_ID = UUID.randomUUID();
    
    @Test
    @DisplayName("Should create notification with valid inputs")
    void shouldCreateNotification() {
        Notification notification = Notification.create(
            USER_ID,
            NotificationType.ORDER_CONFIRMATION,
            NotificationChannel.EMAIL,
            "Order Confirmed",
            "Your order has been confirmed.",
            Map.of("orderId", "123")
        );
        
        assertNotNull(notification.id());
        assertEquals(USER_ID, notification.userId());
        assertEquals(NotificationType.ORDER_CONFIRMATION, notification.type());
        assertEquals(NotificationChannel.EMAIL, notification.channel());
        assertEquals("Order Confirmed", notification.subject());
        assertEquals(NotificationStatus.PENDING, notification.status());
        assertNotNull(notification.createdAt());
    }
    
    @Test
    @DisplayName("Should throw exception for null user ID")
    void shouldThrowForNullUserId() {
        assertThrows(IllegalArgumentException.class, () ->
            Notification.create(null, NotificationType.ORDER_CONFIRMATION, 
                NotificationChannel.EMAIL, "Subject", "Message", null));
    }
    
    @Test
    @DisplayName("Should throw exception for null type")
    void shouldThrowForNullType() {
        assertThrows(IllegalArgumentException.class, () ->
            Notification.create(USER_ID, null, NotificationChannel.EMAIL, 
                "Subject", "Message", null));
    }
    
    @Test
    @DisplayName("Should throw exception for blank message")
    void shouldThrowForBlankMessage() {
        assertThrows(IllegalArgumentException.class, () ->
            Notification.create(USER_ID, NotificationType.ORDER_CONFIRMATION, 
                NotificationChannel.EMAIL, "Subject", "  ", null));
    }
    
    @Test
    @DisplayName("Should mark notification as sent")
    void shouldMarkAsSent() {
        Notification notification = Notification.create(
            USER_ID, NotificationType.ORDER_CONFIRMATION, NotificationChannel.EMAIL,
            "Subject", "Message", null
        );
        
        Notification sent = notification.markSent();
        
        assertEquals(NotificationStatus.SENT, sent.status());
        assertNotNull(sent.sentAt());
        assertEquals(NotificationStatus.PENDING, notification.status());
    }
    
    @Test
    @DisplayName("Should mark notification as failed")
    void shouldMarkAsFailed() {
        Notification notification = Notification.create(
            USER_ID, NotificationType.ORDER_CONFIRMATION, NotificationChannel.EMAIL,
            "Subject", "Message", null
        );
        
        Notification failed = notification.markFailed();
        
        assertEquals(NotificationStatus.FAILED, failed.status());
        assertNull(failed.sentAt());
    }
    
    @Test
    @DisplayName("Should create scheduled notification")
    void shouldCreateScheduledNotification() {
        java.time.Instant scheduledTime = java.time.Instant.now().plusSeconds(3600);
        
        Notification notification = Notification.scheduled(
            USER_ID, NotificationType.PROMOTIONAL, NotificationChannel.PUSH,
            "Special Offer", "Check out our deals", scheduledTime
        );
        
        assertEquals(NotificationStatus.SCHEDULED, notification.status());
        assertEquals(scheduledTime, notification.scheduledAt());
    }
}
