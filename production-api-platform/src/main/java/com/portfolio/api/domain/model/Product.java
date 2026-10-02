package com.portfolio.api.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Product domain model representing a catalog item.
 * Immutable record for thread-safety and clear semantics.
 */
public record Product(
    UUID id,
    String name,
    String description,
    String category,
    BigDecimal price,
    String currency,
    int stockQuantity,
    ProductStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    
    public Product {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product price must be non-negative");
        }
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
    }
    
    public static Product create(String name, String description, String category, 
                                  BigDecimal price, String currency, int stockQuantity) {
        Instant now = Instant.now();
        return new Product(
            UUID.randomUUID(),
            name,
            description,
            category,
            price,
            currency != null ? currency : "USD",
            stockQuantity,
            ProductStatus.ACTIVE,
            now,
            now
        );
    }
    
    public Product withUpdatedStock(int newQuantity) {
        return new Product(
            this.id,
            this.name,
            this.description,
            this.category,
            this.price,
            this.currency,
            newQuantity,
            newQuantity > 0 ? ProductStatus.ACTIVE : ProductStatus.OUT_OF_STOCK,
            this.createdAt,
            Instant.now()
        );
    }
    
    public Product withUpdatedPrice(BigDecimal newPrice) {
        return new Product(
            this.id,
            this.name,
            this.description,
            this.category,
            newPrice,
            this.currency,
            this.stockQuantity,
            this.status,
            this.createdAt,
            Instant.now()
        );
    }
    
    public boolean isAvailable() {
        return status == ProductStatus.ACTIVE && stockQuantity > 0;
    }
}
