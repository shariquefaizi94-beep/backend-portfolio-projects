package com.nexaforge.payment.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment record with idempotent processing via unique orderId constraint.
 * Tracks authorization, capture, and refund lifecycle.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order_id", columnList = "orderId", unique = true),
        @Index(name = "idx_payment_status", columnList = "status")
})
public class Payment {

    @Id
    private String transactionId;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Column(nullable = false, length = 32)
    private String status; // AUTHORIZED, CAPTURED, REFUNDED, FAILED

    @Version
    private Long version;

    @Column(length = 1024)
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected Payment() {}

    public static Payment create(String orderId, String customerId, BigDecimal amount, String currency) {
        Payment p = new Payment();
        p.transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        p.orderId = orderId;
        p.customerId = customerId;
        p.amount = amount;
        p.currency = currency != null ? currency : "USD";
        p.status = "AUTHORIZED";
        p.createdAt = Instant.now();
        p.updatedAt = Instant.now();
        return p;
    }

    public void capture() { this.status = "CAPTURED"; this.updatedAt = Instant.now(); }
    public void refund() { this.status = "REFUNDED"; this.updatedAt = Instant.now(); }
    public void fail(String reason) {
        this.status = "FAILED";
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public String getTransactionId() { return transactionId; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
