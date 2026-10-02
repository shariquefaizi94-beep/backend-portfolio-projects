package com.nexaforge.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexaforge.order.domain.enums.OrderStatus;
import com.nexaforge.order.domain.model.Order;
import com.nexaforge.order.exception.OrderNotFoundException;
import com.nexaforge.order.outbox.OutboxRepository;
import com.nexaforge.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OutboxRepository outboxRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        orderService = new OrderService(orderRepository, outboxRepository, mapper);
        // Inject kafka topic via reflection since @Value won't work in unit test
        try {
            var field = OrderService.class.getDeclaredField("orderEventsTopic");
            field.setAccessible(true);
            field.set(orderService, "order.events");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    @DisplayName("Should create new order with items and publish to outbox")
    void shouldCreateOrder() {
        when(orderRepository.findByIdempotencyKey("key-001")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var items = List.of(new OrderService.CreateOrderItemRequest("SKU-001", 2, BigDecimal.valueOf(25.00)));
        Order order = orderService.createOrder("key-001", "CUST-001", BigDecimal.valueOf(50.00), "USD", items);

        assertThat(order).isNotNull();
        assertThat(order.getId()).startsWith("ORD-");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PROCESSING);
        assertThat(order.getItems()).hasSize(1);
        verify(outboxRepository).save(any());
    }

    @Test
    @DisplayName("Should return existing order on duplicate idempotencyKey")
    void shouldReturnExistingOnDuplicate() {
        Order existing = Order.create("key-001", "CUST-001", BigDecimal.TEN, "USD");
        when(orderRepository.findByIdempotencyKey("key-001")).thenReturn(Optional.of(existing));

        Order result = orderService.createOrder("key-001", "CUST-001", BigDecimal.TEN, "USD", List.of());

        assertThat(result).isSameAs(existing);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException for unknown ID")
    void shouldThrowNotFound() {
        when(orderRepository.findByIdWithItems("UNKNOWN")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrder("UNKNOWN"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Should advance order on payment completed")
    void shouldHandlePaymentCompleted() {
        Order order = Order.create("key-002", "CUST-002", BigDecimal.valueOf(100), "USD");
        order.transitionTo(OrderStatus.PAYMENT_PROCESSING);
        when(orderRepository.findById("ORD-001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.handlePaymentCompleted("ORD-001", "TXN-123");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
        assertThat(result.getTransactionId()).isEqualTo("TXN-123");
    }
}
