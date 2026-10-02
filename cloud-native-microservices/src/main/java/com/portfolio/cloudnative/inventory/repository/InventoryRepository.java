package com.portfolio.cloudnative.inventory.repository;

import com.portfolio.cloudnative.inventory.model.InventoryItem;
import com.portfolio.cloudnative.inventory.model.InventoryStatus;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for inventory items.
 */
@Repository
public class InventoryRepository {
    
    private final Map<UUID, InventoryItem> inventory = new ConcurrentHashMap<>();
    
    public Optional<InventoryItem> findById(UUID id) {
        return Optional.ofNullable(inventory.get(id));
    }
    
    public Optional<InventoryItem> findByProductId(UUID productId) {
        return inventory.values().stream()
            .filter(i -> i.productId().equals(productId))
            .findFirst();
    }
    
    public Optional<InventoryItem> findBySku(String sku) {
        return inventory.values().stream()
            .filter(i -> i.sku().equalsIgnoreCase(sku))
            .findFirst();
    }
    
    public List<InventoryItem> findAll() {
        return new ArrayList<>(inventory.values());
    }
    
    public List<InventoryItem> findByStatus(InventoryStatus status) {
        return inventory.values().stream()
            .filter(i -> i.status() == status)
            .collect(Collectors.toList());
    }
    
    public List<InventoryItem> findNeedsReorder() {
        return inventory.values().stream()
            .filter(InventoryItem::needsReorder)
            .collect(Collectors.toList());
    }
    
    public InventoryItem save(InventoryItem item) {
        inventory.put(item.id(), item);
        return item;
    }
    
    public void deleteById(UUID id) {
        inventory.remove(id);
    }
    
    public boolean existsByProductId(UUID productId) {
        return inventory.values().stream()
            .anyMatch(i -> i.productId().equals(productId));
    }
    
    public long count() {
        return inventory.size();
    }
}
