package com.portfolio.cloudnative.inventory.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Inventory item representing stock for a product.
 */
public record InventoryItem(
    UUID id,
    UUID productId,
    String sku,
    int quantityOnHand,
    int quantityReserved,
    int reorderPoint,
    int reorderQuantity,
    String warehouseLocation,
    InventoryStatus status,
    Instant lastRestocked,
    Instant updatedAt
) {
    
    public InventoryItem {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (quantityOnHand < 0) {
            throw new IllegalArgumentException("Quantity on hand cannot be negative");
        }
        if (quantityReserved < 0) {
            throw new IllegalArgumentException("Quantity reserved cannot be negative");
        }
    }
    
    public static InventoryItem create(UUID productId, String sku, int quantity, 
                                        String warehouseLocation, int reorderPoint, int reorderQuantity) {
        Instant now = Instant.now();
        return new InventoryItem(
            UUID.randomUUID(),
            productId,
            sku,
            quantity,
            0,
            reorderPoint,
            reorderQuantity,
            warehouseLocation,
            quantity > 0 ? InventoryStatus.IN_STOCK : InventoryStatus.OUT_OF_STOCK,
            now,
            now
        );
    }
    
    public int availableQuantity() {
        return Math.max(0, quantityOnHand - quantityReserved);
    }
    
    public boolean needsReorder() {
        return availableQuantity() <= reorderPoint;
    }
    
    public InventoryItem reserve(int quantity) {
        if (quantity > availableQuantity()) {
            throw new IllegalStateException("Cannot reserve more than available quantity");
        }
        return new InventoryItem(
            id, productId, sku, quantityOnHand, quantityReserved + quantity,
            reorderPoint, reorderQuantity, warehouseLocation, status, lastRestocked, Instant.now()
        );
    }
    
    public InventoryItem releaseReservation(int quantity) {
        int newReserved = Math.max(0, quantityReserved - quantity);
        return new InventoryItem(
            id, productId, sku, quantityOnHand, newReserved,
            reorderPoint, reorderQuantity, warehouseLocation, status, lastRestocked, Instant.now()
        );
    }
    
    public InventoryItem adjustQuantity(int adjustment) {
        int newQuantity = Math.max(0, quantityOnHand + adjustment);
        InventoryStatus newStatus = newQuantity > reorderPoint ? InventoryStatus.IN_STOCK :
                                    newQuantity > 0 ? InventoryStatus.LOW_STOCK : InventoryStatus.OUT_OF_STOCK;
        return new InventoryItem(
            id, productId, sku, newQuantity, quantityReserved,
            reorderPoint, reorderQuantity, warehouseLocation, newStatus, 
            adjustment > 0 ? Instant.now() : lastRestocked, Instant.now()
        );
    }
}
