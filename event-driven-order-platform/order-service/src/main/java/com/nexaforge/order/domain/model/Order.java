package com.nexaforge.order.domain.model;

import com.nexaforge.order.domain.enums.OrderStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Root aggregate for order. Owns the order lifecycle state machine.
 * Uses optimistic locking to prevent concurrent saga step conflicts.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_customer_id", columnList = "customerId"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_idempotency_key", columnList = "idempotencyKey", unique = true)
})
public class Order {

    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Version
    private Long version;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 3)
    private String currency;

    private String transactionId;
    private String reservationId;
    private String shipmentId;
    private String trackingNumber;

    @Column(length = 1024)
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;
    private Instant completedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {}

    public static Order create(String idempotencyKey, String customerId,
                               BigDecimal totalAmount, String currency) {
        Order order = new Order();
        order.id = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        order.idempotencyKey = idempotencyKey;
        order.customerId = customerId;
        order.status = OrderStatus.PENDING;
        order.totalAmount = totalAmount;
        order.currency = currency != null ? currency : "USD";
        order.createdAt = Instant.now();
        order.updatedAt = Instant.now();
        return order;
    }

    public void transitionTo(OrderStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition order %s from %s to %s".formatted(id, status, newStatus));
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
        if (newStatus.isTerminal()) {
            this.completedAt = Instant.now();
        }
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void markPaymentCompleted(String transactionId) {
        this.transactionId = transactionId;
        transitionTo(OrderStatus.PAYMENT_COMPLETED);
    }

    public void markInventoryReserved(String reservationId) {
        this.reservationId = reservationId;
        transitionTo(OrderStatus.INVENTORY_RESERVED);
    }

    public void markShippingArranged(String shipmentId, String trackingNumber) {
        this.shipmentId = shipmentId;
        this.trackingNumber = trackingNumber;
        transitionTo(OrderStatus.SHIPPING_ARRANGED);
    }

    public void markCompleted() { transitionTo(OrderStatus.COMPLETED); }

    public void fail(String reason) {
        this.failureReason = reason;
        if (status.canTransitionTo(OrderStatus.FAILED)) {
            transitionTo(OrderStatus.FAILED);
        }
    }

    // Getters
    public String getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public String getTransactionId() { return transactionId; }
    public String getReservationId() { return reservationId; }
    public String getShipmentId() { return shipmentId; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
}
