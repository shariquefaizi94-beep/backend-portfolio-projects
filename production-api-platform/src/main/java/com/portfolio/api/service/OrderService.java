package com.portfolio.api.service;

import com.portfolio.api.domain.model.*;
import com.portfolio.api.repository.OrderRepository;
import com.portfolio.api.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for Order operations.
 */
@Service
public class OrderService {
    
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    
    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }
    
    public Optional<Order> findById(UUID id) {
        log.debug("Finding order by id: {}", id);
        return orderRepository.findById(id);
    }
    
    public List<Order> findByUserId(UUID userId) {
        log.debug("Finding orders for user: {}", userId);
        return orderRepository.findByUserId(userId);
    }
    
    public List<Order> findByStatus(OrderStatus status) {
        log.debug("Finding orders by status: {}", status);
        return orderRepository.findByStatus(status);
    }
    
    public List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status) {
        log.debug("Finding orders for user {} with status {}", userId, status);
        return orderRepository.findByUserIdAndStatus(userId, status);
    }
    
    /**
     * Creates a new order from a list of product IDs and quantities.
     */
    public Order createOrder(UUID userId, List<OrderItemInput> itemInputs, 
                              String shippingAddress, String billingAddress) {
        log.info("Creating order for user: {} with {} items", userId, itemInputs.size());
        
        List<OrderItem> items = new ArrayList<>();
        
        for (OrderItemInput input : itemInputs) {
            Product product = productRepository.findById(input.productId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Product not found: " + input.productId()));
            
            if (!product.isAvailable()) {
                throw new IllegalStateException("Product not available: " + product.name());
            }
            
            if (product.stockQuantity() < input.quantity()) {
                throw new IllegalStateException(
                    "Insufficient stock for product: " + product.name() + 
                    ". Available: " + product.stockQuantity() + ", Requested: " + input.quantity());
            }
            
            items.add(new OrderItem(
                product.id(),
                product.name(),
                input.quantity(),
                product.price()
            ));
            
            // Update stock
            productRepository.save(product.withUpdatedStock(
                product.stockQuantity() - input.quantity()));
        }
        
        Order order = Order.create(userId, items, shippingAddress, billingAddress);
        return orderRepository.save(order);
    }
    
    public Optional<Order> updateStatus(UUID orderId, OrderStatus newStatus) {
        log.info("Updating order {} status to: {}", orderId, newStatus);
        return orderRepository.findById(orderId)
            .map(order -> order.withStatus(newStatus))
            .map(orderRepository::save);
    }
    
    public Optional<Order> cancelOrder(UUID orderId) {
        log.info("Cancelling order: {}", orderId);
        
        return orderRepository.findById(orderId)
            .map(order -> {
                if (order.status() == OrderStatus.SHIPPED || 
                    order.status() == OrderStatus.DELIVERED) {
                    throw new IllegalStateException(
                        "Cannot cancel order that has been shipped or delivered");
                }
                
                // Restore stock for cancelled orders
                for (OrderItem item : order.items()) {
                    productRepository.findById(item.productId())
                        .ifPresent(product -> 
                            productRepository.save(product.withUpdatedStock(
                                product.stockQuantity() + item.quantity())));
                }
                
                return orderRepository.save(order.withStatus(OrderStatus.CANCELLED));
            });
    }
    
    public long count() {
        return orderRepository.count();
    }
    
    public long countByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }
    
    public List<Order> findPaginated(int page, int size) {
        return orderRepository.findAllPaginated(page, size);
    }
    
    /**
     * Input record for creating order items.
     */
    public record OrderItemInput(UUID productId, int quantity) {
        public OrderItemInput {
            if (productId == null) {
                throw new IllegalArgumentException("Product ID cannot be null");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
        }
    }
}
