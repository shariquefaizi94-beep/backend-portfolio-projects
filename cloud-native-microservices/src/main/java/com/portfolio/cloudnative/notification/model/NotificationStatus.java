package com.portfolio.cloudnative.notification.model;

/**
 * Status of a notification.
 */
public enum NotificationStatus {
    PENDING,
    SCHEDULED,
    SENT,
    DELIVERED,
    FAILED,
    CANCELLED
}
