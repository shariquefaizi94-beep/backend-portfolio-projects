package com.nexaforge.common.event;

/**
 * Published when shipping is arranged. Final step in the saga.
 * Consumed by: order-service (to complete order), notification-service.
 */
public class ShippingArrangedEvent extends BaseEvent {

    private String orderId;
    private String shipmentId;
    private String trackingNumber;
    private String carrier;
    private int estimatedDays;

    public ShippingArrangedEvent() { super(); }

    public ShippingArrangedEvent(String correlationId, String orderId, String shipmentId,
                                 String trackingNumber, String carrier, int estimatedDays) {
        super(correlationId);
        this.orderId = orderId;
        this.shipmentId = shipmentId;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.estimatedDays = estimatedDays;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }
    public int getEstimatedDays() { return estimatedDays; }
    public void setEstimatedDays(int estimatedDays) { this.estimatedDays = estimatedDays; }
}
