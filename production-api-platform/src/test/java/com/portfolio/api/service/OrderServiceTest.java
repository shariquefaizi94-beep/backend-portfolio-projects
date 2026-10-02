package com.portfolio.api.service;

import com.portfolio.api.domain.model.Order;
import com.portfolio.api.domain.model.OrderItem;
import com.portfolio.api.domain.model.OrderStatus;
import com.portfolio.api.domain.model.Product;
import com.portfolio.api.domain.model.ProductStatus;
import com.portfolio.api.repository.OrderRepository;
import com.portfolio.api.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Service Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private ProductRepository productRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, productRepository);
    }

    @Test
    @DisplayName("Should find order by ID")
    void shouldFindOrderById() {
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Optional<Order> result = orderService.findById(orderId);

        assertTrue(result.isPresent());
        assertEquals(orderId, result.get().id());
    }

    @Test
    @DisplayName("Should find orders by user ID")
    void shouldFindOrdersByUserId() {
        UUID userId = UUID.randomUUID();
        List<Order> orders = List.of(
            createOrder(UUID.randomUUID()),
            createOrder(UUID.randomUUID())
        );
        when(orderRepository.findByUserId(userId)).thenReturn(orders);

        List<Order> result = orderService.findByUserId(userId);

        assertEquals(2, result.size());
        verify(orderRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("Should find orders by status")
    void shouldFindOrdersByStatus() {
        List<Order> orders = List.of(createOrder(UUID.randomUUID()));
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(orders);

        List<Order> result = orderService.findByStatus(OrderStatus.PENDING);

        assertEquals(1, result.size());
        verify(orderRepository).findByStatus(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("Should create order successfully")
    void shouldCreateOrderSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = createProduct(productId, "Test Product", 100, new BigDecimal("25.00"));
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<OrderService.OrderItemInput> items = List.of(
            new OrderService.OrderItemInput(productId, 2)
        );

        Order result = orderService.createOrder(userId, items, "123 Main St", "123 Main St");

        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals(1, result.items().size());
        assertEquals(OrderStatus.PENDING, result.status());
        verify(productRepository).save(any(Product.class)); // Stock updated
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        List<OrderService.OrderItemInput> items = List.of(
            new OrderService.OrderItemInput(productId, 2)
        );

        assertThrows(IllegalArgumentException.class, () ->
            orderService.createOrder(userId, items, "Address", "Address"));
    }

    @Test
    @DisplayName("Should throw exception when insufficient stock")
    void shouldThrowExceptionWhenInsufficientStock() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = createProduct(productId, "Test Product", 5, new BigDecimal("25.00"));
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        List<OrderService.OrderItemInput> items = List.of(
            new OrderService.OrderItemInput(productId, 10) // Requesting more than available
        );

        assertThrows(IllegalStateException.class, () ->
            orderService.createOrder(userId, items, "Address", "Address"));
    }

    @Test
    @DisplayName("Should update order status")
    void shouldUpdateOrderStatus() {
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Order> result = orderService.updateStatus(orderId, OrderStatus.CONFIRMED);

        assertTrue(result.isPresent());
        assertEquals(OrderStatus.CONFIRMED, result.get().status());
    }

    @Test
    @DisplayName("Should cancel order and restore stock")
    void shouldCancelOrderAndRestoreStock() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Order order = createOrderWithProduct(orderId, productId, 2);
        Product product = createProduct(productId, "Test", 98, new BigDecimal("10.00"));
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Order> result = orderService.cancelOrder(orderId);

        assertTrue(result.isPresent());
        assertEquals(OrderStatus.CANCELLED, result.get().status());
        verify(productRepository).save(any(Product.class)); // Stock restored
    }

    @Test
    @DisplayName("Should throw exception when cancelling shipped order")
    void shouldThrowExceptionWhenCancellingShippedOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = createOrderWithStatus(orderId, OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () ->
            orderService.cancelOrder(orderId));
    }

    private Order createOrder(UUID orderId) {
        return createOrderWithProduct(orderId, UUID.randomUUID(), 1);
    }
    
    private Order createOrderWithProduct(UUID orderId, UUID productId, int quantity) {
        Instant now = Instant.now();
        List<OrderItem> items = List.of(
            new OrderItem(productId, "Test Product", quantity, new BigDecimal("10.00"))
        );
        return new Order(
            orderId,
            UUID.randomUUID(),
            items,
            new BigDecimal("10.00"),
            new BigDecimal("0.80"),
            new BigDecimal("10.80"),
            OrderStatus.PENDING,
            "Address",
            "Address",
            now,
            now
        );
    }
    
    private Order createOrderWithStatus(UUID orderId, OrderStatus status) {
        Instant now = Instant.now();
        List<OrderItem> items = List.of(
            new OrderItem(UUID.randomUUID(), "Test Product", 1, new BigDecimal("10.00"))
        );
        return new Order(
            orderId,
            UUID.randomUUID(),
            items,
            new BigDecimal("10.00"),
            new BigDecimal("0.80"),
            new BigDecimal("10.80"),
            status,
            "Address",
            "Address",
            now,
            now
        );
    }

    private Product createProduct(UUID id, String name, int stock, BigDecimal price) {
        Instant now = Instant.now();
        return new Product(
            id,
            name,
            "Description",
            "Electronics",
            price,
            "USD",
            stock,
            ProductStatus.ACTIVE,
            now,
            now
        );
    }
}
