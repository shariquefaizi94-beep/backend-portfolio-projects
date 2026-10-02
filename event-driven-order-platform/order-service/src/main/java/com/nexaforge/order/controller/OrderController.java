package com.nexaforge.order.controller;

import com.nexaforge.order.domain.enums.OrderStatus;
import com.nexaforge.order.domain.model.Order;
import com.nexaforge.order.service.OrderService;
import com.nexaforge.order.service.OrderService.CreateOrderItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order lifecycle management")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Place a new order (idempotent)")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        List<CreateOrderItemRequest> items = request.items().stream()
                .map(i -> new CreateOrderItemRequest(i.sku(), i.quantity(), i.price()))
                .toList();

        Order order = orderService.createOrder(
                request.idempotencyKey(), request.customerId(),
                request.totalAmount(), request.currency(), items);

        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(OrderResponse.from(orderService.getOrder(id)));
    }

    @GetMapping
    @Operation(summary = "List orders with optional status filter")
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(orderService.listOrders(status, pageable).map(OrderResponse::from));
    }

    // ─── DTOs ────────────────────────────────────

    record CreateOrderRequest(
            @NotBlank String idempotencyKey,
            @NotBlank String customerId,
            @Positive BigDecimal totalAmount,
            String currency,
            @NotEmpty List<ItemRequest> items
    ) {}

    record ItemRequest(@NotBlank String sku, @Positive int quantity, @Positive BigDecimal price) {}

    record OrderResponse(
            String id, String customerId, String status, BigDecimal totalAmount,
            String currency, String transactionId, String reservationId,
            String shipmentId, String trackingNumber, String failureReason,
            Instant createdAt, Instant completedAt, List<ItemResponse> items
    ) {
        static OrderResponse from(Order o) {
            List<ItemResponse> items = o.getItems().stream()
                    .map(i -> new ItemResponse(i.getSku(), i.getQuantity(), i.getPrice()))
                    .toList();
            return new OrderResponse(
                    o.getId(), o.getCustomerId(), o.getStatus().name(), o.getTotalAmount(),
                    o.getCurrency(), o.getTransactionId(), o.getReservationId(),
                    o.getShipmentId(), o.getTrackingNumber(), o.getFailureReason(),
                    o.getCreatedAt(), o.getCompletedAt(), items);
        }
    }

    record ItemResponse(String sku, int quantity, BigDecimal price) {}
}
