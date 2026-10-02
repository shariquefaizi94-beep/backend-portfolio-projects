package com.portfolio.cloudnative.notification.client;

import com.portfolio.cloudnative.notification.model.*;
import com.portfolio.cloudnative.notification.service.NotificationService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Client for sending notifications from other microservices.
 * In production, this would be a Feign client calling the notification service.
 * For demo purposes, it directly calls the NotificationService.
 */
@Component
public class NotificationClient {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
    
    private final NotificationService notificationService;
    
    public NotificationClient(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @CircuitBreaker(name = "notificationClient", fallbackMethod = "sendLowStockAlertFallback")
    public void sendLowStockAlert(UUID productId, String sku, int availableQuantity) {
        log.info("Sending low stock alert for product: {} ({}), quantity: {}", 
                productId, sku, availableQuantity);
        
        // In production, this would send to an admin user or group
        UUID adminUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        
        notificationService.create(
            adminUserId,
            NotificationType.INVENTORY_LOW,
            NotificationChannel.EMAIL,
            "Low Stock Alert: " + sku,
            String.format("Product %s (SKU: %s) is low on stock. Only %d units available.", 
                         productId, sku, availableQuantity),
            Map.of(
                "productId", productId.toString(),
                "sku", sku,
                "availableQuantity", String.valueOf(availableQuantity)
            )
        );
    }
    
    public void sendLowStockAlertFallback(UUID productId, String sku, int availableQuantity, Exception e) {
        log.error("Failed to send low stock alert for product {}: {}", productId, e.getMessage());
        // In production, this might queue the notification for retry or send to a dead-letter queue
    }
    
    @CircuitBreaker(name = "notificationClient", fallbackMethod = "sendOrderConfirmationFallback")
    public void sendOrderConfirmation(UUID userId, UUID orderId, String orderDetails) {
        log.info("Sending order confirmation to user: {} for order: {}", userId, orderId);
        
        notificationService.create(
            userId,
            NotificationType.ORDER_CONFIRMATION,
            NotificationChannel.EMAIL,
            "Order Confirmation - " + orderId,
            orderDetails,
            Map.of("orderId", orderId.toString())
        );
    }
    
    public void sendOrderConfirmationFallback(UUID userId, UUID orderId, String orderDetails, Exception e) {
        log.error("Failed to send order confirmation for order {}: {}", orderId, e.getMessage());
    }
    
    @CircuitBreaker(name = "notificationClient", fallbackMethod = "sendAccountCreatedFallback")
    public void sendAccountCreated(UUID userId, String username, String email) {
        log.info("Sending account created notification to: {}", email);
        
        notificationService.create(
            userId,
            NotificationType.ACCOUNT_CREATED,
            NotificationChannel.EMAIL,
            "Welcome to Our Platform!",
            String.format("Hello %s, your account has been created successfully.", username),
            Map.of("username", username, "email", email)
        );
    }
    
    public void sendAccountCreatedFallback(UUID userId, String username, String email, Exception e) {
        log.error("Failed to send account created notification to {}: {}", email, e.getMessage());
    }
}
