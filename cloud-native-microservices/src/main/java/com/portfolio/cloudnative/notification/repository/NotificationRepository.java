package com.portfolio.cloudnative.notification.repository;

import com.portfolio.cloudnative.notification.model.Notification;
import com.portfolio.cloudnative.notification.model.NotificationStatus;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for notifications.
 */
@Repository
public class NotificationRepository {
    
    private final Map<UUID, Notification> notifications = new ConcurrentHashMap<>();
    
    public Optional<Notification> findById(UUID id) {
        return Optional.ofNullable(notifications.get(id));
    }
    
    public List<Notification> findByUserId(UUID userId) {
        return notifications.values().stream()
            .filter(n -> n.userId().equals(userId))
            .sorted(Comparator.comparing(Notification::createdAt).reversed())
            .collect(Collectors.toList());
    }
    
    public List<Notification> findByStatus(NotificationStatus status) {
        return notifications.values().stream()
            .filter(n -> n.status() == status)
            .collect(Collectors.toList());
    }
    
    public Notification save(Notification notification) {
        notifications.put(notification.id(), notification);
        return notification;
    }
    
    public void deleteById(UUID id) {
        notifications.remove(id);
    }
    
    public long count() {
        return notifications.size();
    }
    
    public long countByStatus(NotificationStatus status) {
        return notifications.values().stream()
            .filter(n -> n.status() == status)
            .count();
    }
}
