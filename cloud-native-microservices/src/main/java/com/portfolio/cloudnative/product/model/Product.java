package com.portfolio.cloudnative.product.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Product domain model for the product microservice.
 */
public record Product(
    UUID id,
    String sku,
    String name,
    String description,
    String category,
    BigDecimal price,
    String currency,
    ProductStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    
    public Product {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be non-negative");
        }
    }
    
    public static Product create(String sku, String name, String description, 
                                  String category, BigDecimal price, String currency) {
        Instant now = Instant.now();
        return new Product(
            UUID.randomUUID(),
            sku,
            name,
            description,
            category,
            price,
            currency != null ? currency : "USD",
            ProductStatus.ACTIVE,
            now,
            now
        );
    }
    
    public Product withPrice(BigDecimal newPrice) {
        return new Product(id, sku, name, description, category, newPrice, currency, status, createdAt, Instant.now());
    }
    
    public Product withStatus(ProductStatus newStatus) {
        return new Product(id, sku, name, description, category, price, currency, newStatus, createdAt, Instant.now());
    }
}
