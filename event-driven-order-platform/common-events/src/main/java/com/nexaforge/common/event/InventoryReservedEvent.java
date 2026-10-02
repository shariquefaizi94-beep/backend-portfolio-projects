package com.nexaforge.common.event;

import java.util.List;

/**
 * Published when inventory is successfully reserved.
 * Consumed by: order-service (saga), shipping-service.
 */
public class InventoryReservedEvent extends BaseEvent {

    private String orderId;
    private String reservationId;
    private List<ReservedItem> reservedItems;

    public InventoryReservedEvent() { super(); }

    public InventoryReservedEvent(String correlationId, String orderId, String reservationId,
                                  List<ReservedItem> reservedItems) {
        super(correlationId);
        this.orderId = orderId;
        this.reservationId = reservationId;
        this.reservedItems = reservedItems;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public List<ReservedItem> getReservedItems() { return reservedItems; }
    public void setReservedItems(List<ReservedItem> reservedItems) { this.reservedItems = reservedItems; }

    public record ReservedItem(String sku, int quantity, String warehouseId) {}
}
