package com.nexaforge.common.event;

/**
 * Published when payment fails. Triggers compensation in the saga.
 * Consumed by: order-service (to mark order failed), notification-service.
 */
public class PaymentFailedEvent extends BaseEvent {

    private String orderId;
    private String reason;

    public PaymentFailedEvent() { super(); }

    public PaymentFailedEvent(String correlationId, String orderId, String reason) {
        super(correlationId);
        this.orderId = orderId;
        this.reason = reason;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
