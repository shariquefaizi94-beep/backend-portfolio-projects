package com.nexaforge.common.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Published when a new order is placed.
 * Consumed by: payment-service, inventory-service, notification-service.
 */
public class OrderCreatedEvent extends BaseEvent {

    private String orderId;
    private String customerId;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItem> items;

    public OrderCreatedEvent() { super(); }

    public OrderCreatedEvent(String correlationId, String orderId, String customerId,
                             BigDecimal totalAmount, String currency, List<OrderItem> items) {
        super(correlationId);
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.items = items;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public record OrderItem(String sku, int quantity, BigDecimal price) {}
}
