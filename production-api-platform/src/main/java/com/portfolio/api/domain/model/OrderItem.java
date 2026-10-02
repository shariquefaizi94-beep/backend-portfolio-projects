package com.portfolio.api.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Order item representing a product in an order.
 */
public record OrderItem(
    UUID productId,
    String productName,
    int quantity,
    BigDecimal unitPrice
) {
    
    public OrderItem {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price must be non-negative");
        }
    }
    
    public BigDecimal totalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
