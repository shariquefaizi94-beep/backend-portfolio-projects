package com.portfolio.api.repository;

import com.portfolio.api.domain.model.Order;
import com.portfolio.api.domain.model.OrderStatus;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of OrderRepository.
 * Provides thread-safe operations for demonstration.
 */
@Repository
public class InMemoryOrderRepository implements OrderRepository {
    
    private final Map<UUID, Order> orders = new ConcurrentHashMap<>();
    
    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(orders.get(id));
    }
    
    @Override
    public List<Order> findByUserId(UUID userId) {
        return orders.values().stream()
            .filter(o -> o.userId().equals(userId))
            .sorted(Comparator.comparing(Order::createdAt).reversed())
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return orders.values().stream()
            .filter(o -> o.status() == status)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status) {
        return orders.values().stream()
            .filter(o -> o.userId().equals(userId) && o.status() == status)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Order> findByCreatedAtBetween(Instant start, Instant end) {
        return orders.values().stream()
            .filter(o -> !o.createdAt().isBefore(start) && !o.createdAt().isAfter(end))
            .sorted(Comparator.comparing(Order::createdAt))
            .collect(Collectors.toList());
    }
    
    @Override
    public Order save(Order order) {
        orders.put(order.id(), order);
        return order;
    }
    
    @Override
    public void deleteById(UUID id) {
        orders.remove(id);
    }
    
    @Override
    public boolean existsById(UUID id) {
        return orders.containsKey(id);
    }
    
    @Override
    public long count() {
        return orders.size();
    }
    
    @Override
    public long countByStatus(OrderStatus status) {
        return orders.values().stream()
            .filter(o -> o.status() == status)
            .count();
    }
    
    @Override
    public List<Order> findAllPaginated(int page, int size) {
        return orders.values().stream()
            .sorted(Comparator.comparing(Order::createdAt).reversed())
            .skip((long) page * size)
            .limit(size)
            .collect(Collectors.toList());
    }
}
