package com.portfolio.api.domain.model;

/**
 * Order status enumeration representing order lifecycle.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
