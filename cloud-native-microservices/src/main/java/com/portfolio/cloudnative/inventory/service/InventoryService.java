package com.portfolio.cloudnative.inventory.service;

import com.portfolio.cloudnative.inventory.model.InventoryItem;
import com.portfolio.cloudnative.inventory.model.InventoryStatus;
import com.portfolio.cloudnative.inventory.repository.InventoryRepository;
import com.portfolio.cloudnative.notification.client.NotificationClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inventory service with circuit breaker, retry, and async notification patterns.
 */
@Service
public class InventoryService {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    
    private final InventoryRepository inventoryRepository;
    private final NotificationClient notificationClient;
    
    public InventoryService(InventoryRepository inventoryRepository, 
                            NotificationClient notificationClient) {
        this.inventoryRepository = inventoryRepository;
        this.notificationClient = notificationClient;
    }
    
    @Timed(value = "inventory.service.findById")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "findByIdFallback")
    @Retry(name = "inventoryService")
    public Optional<InventoryItem> findById(UUID id) {
        log.debug("Finding inventory item by ID: {}", id);
        return inventoryRepository.findById(id);
    }
    
    public Optional<InventoryItem> findByIdFallback(UUID id, Exception e) {
        log.warn("Circuit breaker fallback for findById: {}", e.getMessage());
        return Optional.empty();
    }
    
    @Timed(value = "inventory.service.findByProductId")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "findByProductIdFallback")
    public Optional<InventoryItem> findByProductId(UUID productId) {
        log.debug("Finding inventory by product ID: {}", productId);
        return inventoryRepository.findByProductId(productId);
    }
    
    public Optional<InventoryItem> findByProductIdFallback(UUID productId, Exception e) {
        log.warn("Circuit breaker fallback for findByProductId: {}", e.getMessage());
        return Optional.empty();
    }
    
    @Timed(value = "inventory.service.findLowStock")
    public List<InventoryItem> findLowStock() {
        return inventoryRepository.findByStatus(InventoryStatus.LOW_STOCK);
    }
    
    @Timed(value = "inventory.service.findNeedsReorder")
    public List<InventoryItem> findNeedsReorder() {
        return inventoryRepository.findNeedsReorder();
    }
    
    @Timed(value = "inventory.service.create")
    public InventoryItem create(UUID productId, String sku, int quantity, 
                                 String warehouseLocation, int reorderPoint, int reorderQuantity) {
        log.info("Creating inventory for product: {} with quantity: {}", productId, quantity);
        
        if (inventoryRepository.existsByProductId(productId)) {
            throw new IllegalArgumentException("Inventory already exists for product: " + productId);
        }
        
        InventoryItem item = InventoryItem.create(productId, sku, quantity, 
            warehouseLocation, reorderPoint, reorderQuantity);
        return inventoryRepository.save(item);
    }
    
    @Timed(value = "inventory.service.reserve")
    public Optional<InventoryItem> reserve(UUID productId, int quantity) {
        log.info("Reserving {} units for product: {}", quantity, productId);
        
        return inventoryRepository.findByProductId(productId)
            .map(item -> {
                if (item.availableQuantity() < quantity) {
                    throw new IllegalStateException(
                        "Insufficient inventory. Available: " + item.availableQuantity() + 
                        ", Requested: " + quantity);
                }
                return item.reserve(quantity);
            })
            .map(inventoryRepository::save);
    }
    
    @Timed(value = "inventory.service.releaseReservation")
    public Optional<InventoryItem> releaseReservation(UUID productId, int quantity) {
        log.info("Releasing {} reserved units for product: {}", quantity, productId);
        
        return inventoryRepository.findByProductId(productId)
            .map(item -> item.releaseReservation(quantity))
            .map(inventoryRepository::save);
    }
    
    @Timed(value = "inventory.service.adjustQuantity")
    public Optional<InventoryItem> adjustQuantity(UUID productId, int adjustment) {
        log.info("Adjusting inventory for product {} by: {}", productId, adjustment);
        
        return inventoryRepository.findByProductId(productId)
            .map(item -> {
                InventoryItem adjusted = item.adjustQuantity(adjustment);
                
                // Check if low stock notification needed
                if (adjusted.needsReorder() && !item.needsReorder()) {
                    sendLowStockNotification(adjusted);
                }
                
                return adjusted;
            })
            .map(inventoryRepository::save);
    }
    
    @Timed(value = "inventory.service.restock")
    public Optional<InventoryItem> restock(UUID productId, int quantity) {
        log.info("Restocking product {} with {} units", productId, quantity);
        return adjustQuantity(productId, quantity);
    }
    
    private void sendLowStockNotification(InventoryItem item) {
        try {
            notificationClient.sendLowStockAlert(item.productId(), item.sku(), item.availableQuantity());
        } catch (Exception e) {
            log.error("Failed to send low stock notification for product: {}", item.productId(), e);
            // Don't fail the main operation if notification fails
        }
    }
    
    public int getAvailableQuantity(UUID productId) {
        return inventoryRepository.findByProductId(productId)
            .map(InventoryItem::availableQuantity)
            .orElse(0);
    }
    
    public boolean isAvailable(UUID productId, int quantity) {
        return getAvailableQuantity(productId) >= quantity;
    }
    
    public long count() {
        return inventoryRepository.count();
    }
}
