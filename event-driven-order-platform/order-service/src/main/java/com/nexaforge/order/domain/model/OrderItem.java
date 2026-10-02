package com.nexaforge.order.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    protected OrderItem() {}

    public static OrderItem create(String sku, int quantity, BigDecimal price) {
        OrderItem item = new OrderItem();
        item.sku = sku;
        item.quantity = quantity;
        item.price = price;
        return item;
    }

    public UUID getId() { return id; }
    public Order getOrder() { return order; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public void setOrder(Order order) { this.order = order; }
}
