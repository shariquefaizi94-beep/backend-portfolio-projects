package com.nexaforge.common.event;

/**
 * Published when inventory reservation fails (e.g., out of stock).
 * Triggers compensation: payment refund, order cancellation.
 */
public class InventoryReservationFailedEvent extends BaseEvent {

    private String orderId;
    private String reason;

    public InventoryReservationFailedEvent() { super(); }

    public InventoryReservationFailedEvent(String correlationId, String orderId, String reason) {
        super(correlationId);
        this.orderId = orderId;
        this.reason = reason;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
