package com.nexaforge.common.event;

import java.math.BigDecimal;

/**
 * Published when payment is successfully processed.
 * Consumed by: order-service (to advance saga), notification-service.
 */
public class PaymentCompletedEvent extends BaseEvent {

    private String orderId;
    private String transactionId;
    private BigDecimal amount;
    private String status;

    public PaymentCompletedEvent() { super(); }

    public PaymentCompletedEvent(String correlationId, String orderId, String transactionId,
                                 BigDecimal amount, String status) {
        super(correlationId);
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
