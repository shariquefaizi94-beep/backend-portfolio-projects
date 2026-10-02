package com.portfolio.cloudnative.notification.service;

import com.portfolio.cloudnative.notification.model.*;
import com.portfolio.cloudnative.notification.repository.NotificationRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Notification service with async delivery and metrics.
 */
@Service
public class NotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    private final NotificationRepository notificationRepository;
    private final Counter notificationsSent;
    private final Counter notificationsFailed;
    
    public NotificationService(NotificationRepository notificationRepository, 
                                MeterRegistry meterRegistry) {
        this.notificationRepository = notificationRepository;
        this.notificationsSent = meterRegistry.counter("notifications.sent");
        this.notificationsFailed = meterRegistry.counter("notifications.failed");
    }
    
    @Timed(value = "notification.service.findById")
    public Optional<Notification> findById(UUID id) {
        return notificationRepository.findById(id);
    }
    
    @Timed(value = "notification.service.findByUserId")
    public List<Notification> findByUserId(UUID userId) {
        return notificationRepository.findByUserId(userId);
    }
    
    @Timed(value = "notification.service.findPending")
    public List<Notification> findPending() {
        return notificationRepository.findByStatus(NotificationStatus.PENDING);
    }
    
    @Timed(value = "notification.service.create")
    public Notification create(UUID userId, NotificationType type, NotificationChannel channel,
                               String subject, String message, Map<String, String> metadata) {
        log.info("Creating {} notification for user: {} via {}", type, userId, channel);
        Notification notification = Notification.create(userId, type, channel, subject, message, metadata);
        return notificationRepository.save(notification);
    }
    
    @Async
    @Timed(value = "notification.service.sendAsync")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendAsyncFallback")
    public CompletableFuture<Notification> sendAsync(UUID notificationId) {
        log.debug("Sending notification asynchronously: {}", notificationId);
        
        return CompletableFuture.supplyAsync(() -> 
            notificationRepository.findById(notificationId)
                .map(this::deliver)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId))
        );
    }
    
    public CompletableFuture<Notification> sendAsyncFallback(UUID notificationId, Exception e) {
        log.error("Notification send fallback for {}: {}", notificationId, e.getMessage());
        return CompletableFuture.failedFuture(e);
    }
    
    @Timed(value = "notification.service.send")
    public Optional<Notification> send(UUID notificationId) {
        log.debug("Sending notification: {}", notificationId);
        return notificationRepository.findById(notificationId)
            .map(this::deliver);
    }
    
    private Notification deliver(Notification notification) {
        try {
            // Simulate delivery based on channel
            simulateDelivery(notification);
            
            Notification sent = notification.markSent();
            notificationsSent.increment();
            log.info("Notification {} sent successfully via {}", notification.id(), notification.channel());
            return notificationRepository.save(sent);
            
        } catch (Exception e) {
            Notification failed = notification.markFailed();
            notificationsFailed.increment();
            log.error("Failed to send notification {}: {}", notification.id(), e.getMessage());
            return notificationRepository.save(failed);
        }
    }
    
    private void simulateDelivery(Notification notification) {
        // In production, this would integrate with actual delivery services
        log.debug("Simulating {} delivery for notification: {}", 
                 notification.channel(), notification.id());
        
        switch (notification.channel()) {
            case EMAIL -> log.debug("Sending email: {}", notification.subject());
            case SMS -> log.debug("Sending SMS to user: {}", notification.userId());
            case PUSH -> log.debug("Sending push notification: {}", notification.subject());
            case IN_APP -> log.debug("Creating in-app notification");
            case WEBHOOK -> log.debug("Calling webhook endpoint");
        }
    }
    
    @Timed(value = "notification.service.sendBulk")
    public int sendBulk(List<UUID> notificationIds) {
        log.info("Sending {} notifications in bulk", notificationIds.size());
        
        int successCount = 0;
        for (UUID id : notificationIds) {
            try {
                send(id);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send notification {}: {}", id, e.getMessage());
            }
        }
        
        return successCount;
    }
    
    public long countByStatus(NotificationStatus status) {
        return notificationRepository.countByStatus(status);
    }
    
    public long count() {
        return notificationRepository.count();
    }
}
