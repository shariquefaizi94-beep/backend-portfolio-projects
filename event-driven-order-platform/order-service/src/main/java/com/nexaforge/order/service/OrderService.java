package com.nexaforge.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaforge.common.event.OrderCreatedEvent;
import com.nexaforge.order.domain.enums.OrderStatus;
import com.nexaforge.order.domain.model.Order;
import com.nexaforge.order.domain.model.OrderItem;
import com.nexaforge.order.exception.OrderNotFoundException;
import com.nexaforge.order.outbox.OutboxEvent;
import com.nexaforge.order.outbox.OutboxRepository;
import com.nexaforge.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Core order lifecycle service.
 *
 * <p>Uses the transactional outbox pattern: domain changes and their
 * corresponding events are written in the same database transaction.
 * This guarantees no events are lost even if Kafka is temporarily unavailable.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.order-events}")
    private String orderEventsTopic;

    public OrderService(OrderRepository orderRepository,
                        OutboxRepository outboxRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new order and publishes an OrderCreatedEvent via the outbox.
     * Idempotent — returns existing order if idempotencyKey already exists.
     */
    @Transactional
    public Order createOrder(String idempotencyKey, String customerId,
                             BigDecimal totalAmount, String currency,
                             List<CreateOrderItemRequest> items) {
        // Idempotency check
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> {
                    log.info("Returning existing order for idempotencyKey: {}", idempotencyKey);
                    return existing;
                })
                .orElseGet(() -> {
                    Order order = Order.create(idempotencyKey, customerId, totalAmount, currency);

                    for (CreateOrderItemRequest item : items) {
                        order.addItem(OrderItem.create(item.sku(), item.quantity(), item.price()));
                    }

                    order.transitionTo(OrderStatus.PAYMENT_PROCESSING);
                    Order saved = orderRepository.save(order);

                    // Write event to outbox (same transaction as order creation)
                    publishToOutbox(saved, "OrderCreated");

                    log.info("Created order {} for customer {}", saved.getId(), customerId);
                    return saved;
                });
    }

    @Transactional(readOnly = true)
    public Order getOrder(String orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public Page<Order> listOrders(OrderStatus status, Pageable pageable) {
        if (status != null) {
            return orderRepository.findByStatus(status, pageable);
        }
        return orderRepository.findAll(pageable);
    }

    @Transactional
    public Order handlePaymentCompleted(String orderId, String transactionId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.markPaymentCompleted(transactionId);
        return orderRepository.save(order);
    }

    @Transactional
    public Order handlePaymentFailed(String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.fail(reason);
        return orderRepository.save(order);
    }

    @Transactional
    public Order handleInventoryReserved(String orderId, String reservationId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.markInventoryReserved(reservationId);
        return orderRepository.save(order);
    }

    @Transactional
    public Order handleShippingArranged(String orderId, String shipmentId, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.markShippingArranged(shipmentId, trackingNumber);
        order.markCompleted();
        return orderRepository.save(order);
    }

    public record CreateOrderItemRequest(String sku, int quantity, BigDecimal price) {}

    private void publishToOutbox(Order order, String eventType) {
        try {
            List<OrderCreatedEvent.OrderItem> eventItems = order.getItems().stream()
                    .map(i -> new OrderCreatedEvent.OrderItem(i.getSku(), i.getQuantity(), i.getPrice()))
                    .toList();

            OrderCreatedEvent event = new OrderCreatedEvent(
                    order.getId(), order.getId(), order.getCustomerId(),
                    order.getTotalAmount(), order.getCurrency(), eventItems
            );

            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.create(
                    "Order", order.getId(), eventType, orderEventsTopic, payload
            );
            outboxRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Failed to serialize outbox event for order {}", order.getId(), e);
            throw new RuntimeException("Outbox serialization failed", e);
        }
    }
}
