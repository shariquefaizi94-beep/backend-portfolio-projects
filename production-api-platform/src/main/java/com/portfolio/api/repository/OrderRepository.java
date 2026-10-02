package com.portfolio.api.repository;

import com.portfolio.api.domain.model.Order;
import com.portfolio.api.domain.model.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Order persistence operations.
 */
public interface OrderRepository {
    
    Optional<Order> findById(UUID id);
    
    List<Order> findByUserId(UUID userId);
    
    List<Order> findByStatus(OrderStatus status);
    
    List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status);
    
    List<Order> findByCreatedAtBetween(Instant start, Instant end);
    
    Order save(Order order);
    
    void deleteById(UUID id);
    
    boolean existsById(UUID id);
    
    long count();
    
    long countByStatus(OrderStatus status);
    
    List<Order> findAllPaginated(int page, int size);
}
