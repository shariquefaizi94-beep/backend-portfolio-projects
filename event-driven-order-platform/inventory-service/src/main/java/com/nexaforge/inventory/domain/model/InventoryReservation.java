package com.nexaforge.inventory.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Tracks inventory reservations tied to orders.
 * Supports idempotent reservation (unique orderId) and release (compensation).
 */
@Entity
@Table(name = "inventory_reservations", indexes = {
        @Index(name = "idx_reservation_order_id", columnList = "orderId", unique = true)
})
public class InventoryReservation {

    @Id
    private String reservationId;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false, length = 32)
    private String status; // RESERVED, RELEASED, CONFIRMED

    @Version
    private Long version;

    @Column(columnDefinition = "TEXT")
    private String itemsJson;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected InventoryReservation() {}

    public static InventoryReservation create(String orderId, String itemsJson) {
        InventoryReservation r = new InventoryReservation();
        r.reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        r.orderId = orderId;
        r.status = "RESERVED";
        r.itemsJson = itemsJson;
        r.createdAt = Instant.now();
        r.updatedAt = Instant.now();
        return r;
    }

    public void release() { this.status = "RELEASED"; this.updatedAt = Instant.now(); }
    public void confirm() { this.status = "CONFIRMED"; this.updatedAt = Instant.now(); }

    public String getReservationId() { return reservationId; }
    public String getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public String getItemsJson() { return itemsJson; }
    public Instant getCreatedAt() { return createdAt; }
}
