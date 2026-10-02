package com.portfolio.cloudnative.notification.model;

/**
 * Types of notifications that can be sent.
 */
public enum NotificationType {
    ORDER_CONFIRMATION,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    PAYMENT_RECEIVED,
    PAYMENT_FAILED,
    INVENTORY_LOW,
    INVENTORY_RESTOCK,
    ACCOUNT_CREATED,
    PASSWORD_RESET,
    PROMOTIONAL,
    SYSTEM_ALERT
}
