package com.nexaforge.common.event;

/**
 * Published when a saga compensation is triggered.
 * Consumed by: payment-service (refund), inventory-service (release).
 */
public class OrderCompensationEvent extends BaseEvent {

    private String orderId;
    private String reason;
    private String compensationType;

    public OrderCompensationEvent() { super(); }

    public OrderCompensationEvent(String correlationId, String orderId, String reason,
                                  String compensationType) {
        super(correlationId);
        this.orderId = orderId;
        this.reason = reason;
        this.compensationType = compensationType;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getCompensationType() { return compensationType; }
    public void setCompensationType(String compensationType) { this.compensationType = compensationType; }
}
