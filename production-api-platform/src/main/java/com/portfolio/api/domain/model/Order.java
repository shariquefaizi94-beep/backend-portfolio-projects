package com.portfolio.api.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Order domain model representing a customer order.
 */
public record Order(
    UUID id,
    UUID userId,
    List<OrderItem> items,
    BigDecimal subtotal,
    BigDecimal tax,
    BigDecimal total,
    OrderStatus status,
    String shippingAddress,
    String billingAddress,
    Instant createdAt,
    Instant updatedAt
) {
    
    public Order {
        if (userId == null) {
            throw new IllegalArgumentException("Order must have a user ID");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
    }
    
    public static Order create(UUID userId, List<OrderItem> items, 
                                String shippingAddress, String billingAddress) {
        BigDecimal subtotal = items.stream()
            .map(OrderItem::totalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.08")); // 8% tax
        BigDecimal total = subtotal.add(tax);
        
        Instant now = Instant.now();
        return new Order(
            UUID.randomUUID(),
            userId,
            List.copyOf(items),
            subtotal,
            tax,
            total,
            OrderStatus.PENDING,
            shippingAddress,
            billingAddress,
            now,
            now
        );
    }
    
    public Order withStatus(OrderStatus newStatus) {
        return new Order(
            this.id,
            this.userId,
            this.items,
            this.subtotal,
            this.tax,
            this.total,
            newStatus,
            this.shippingAddress,
            this.billingAddress,
            this.createdAt,
            Instant.now()
        );
    }
    
    public int totalItems() {
        return items.stream().mapToInt(OrderItem::quantity).sum();
    }
}
