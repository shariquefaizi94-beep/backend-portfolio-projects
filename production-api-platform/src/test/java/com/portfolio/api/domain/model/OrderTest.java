package com.portfolio.api.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order Domain Model Tests")
class OrderTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Test
    @DisplayName("Should create order with valid inputs")
    void shouldCreateOrderWithValidInputs() {
        List<OrderItem> items = List.of(
            new OrderItem(PRODUCT_ID, "Test Product", 2, new BigDecimal("25.00"))
        );

        Order order = Order.create(USER_ID, items, "123 Main St", "123 Main St");

        assertNotNull(order.id());
        assertEquals(USER_ID, order.userId());
        assertEquals(1, order.items().size());
        assertEquals(OrderStatus.PENDING, order.status());
        assertEquals(0, new BigDecimal("50.00").compareTo(order.subtotal()));
        // Tax is 8% of subtotal
        assertEquals(0, new BigDecimal("4.00").compareTo(order.tax()));
        assertEquals(0, new BigDecimal("54.00").compareTo(order.total()));
        assertEquals(2, order.totalItems());
    }

    @Test
    @DisplayName("Should throw exception for null user ID")
    void shouldThrowExceptionForNullUserId() {
        List<OrderItem> items = List.of(
            new OrderItem(PRODUCT_ID, "Test Product", 1, BigDecimal.TEN)
        );

        assertThrows(IllegalArgumentException.class, () ->
            Order.create(null, items, "Address", "Address"));
    }

    @Test
    @DisplayName("Should throw exception for empty items")
    void shouldThrowExceptionForEmptyItems() {
        assertThrows(IllegalArgumentException.class, () ->
            Order.create(USER_ID, List.of(), "Address", "Address"));
    }

    @Test
    @DisplayName("Should update order status")
    void shouldUpdateOrderStatus() {
        List<OrderItem> items = List.of(
            new OrderItem(PRODUCT_ID, "Test Product", 1, BigDecimal.TEN)
        );

        Order order = Order.create(USER_ID, items, "Address", "Address");
        Order updatedOrder = order.withStatus(OrderStatus.CONFIRMED);

        assertEquals(OrderStatus.CONFIRMED, updatedOrder.status());
        assertEquals(OrderStatus.PENDING, order.status()); // Original unchanged
    }

    @Test
    @DisplayName("Should calculate total items correctly for multiple items")
    void shouldCalculateTotalItemsCorrectly() {
        List<OrderItem> items = List.of(
            new OrderItem(UUID.randomUUID(), "Product 1", 3, new BigDecimal("10.00")),
            new OrderItem(UUID.randomUUID(), "Product 2", 2, new BigDecimal("20.00")),
            new OrderItem(UUID.randomUUID(), "Product 3", 5, new BigDecimal("5.00"))
        );

        Order order = Order.create(USER_ID, items, "Address", "Address");

        assertEquals(10, order.totalItems()); // 3 + 2 + 5
        assertEquals(0, new BigDecimal("95.00").compareTo(order.subtotal())); // 30 + 40 + 25
    }
}
