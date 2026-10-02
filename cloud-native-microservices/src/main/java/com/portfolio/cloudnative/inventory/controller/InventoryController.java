package com.portfolio.cloudnative.inventory.controller;

import com.portfolio.cloudnative.inventory.model.InventoryItem;
import com.portfolio.cloudnative.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for inventory operations.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
    
    private final InventoryService inventoryService;
    
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<InventoryItem> getInventory(@PathVariable UUID id) {
        log.debug("Getting inventory item: {}", id);
        return inventoryService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryItem> getInventoryByProduct(@PathVariable UUID productId) {
        log.debug("Getting inventory for product: {}", productId);
        return inventoryService.findByProductId(productId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/product/{productId}/available")
    public ResponseEntity<Map<String, Object>> getAvailableQuantity(@PathVariable UUID productId) {
        int available = inventoryService.getAvailableQuantity(productId);
        Map<String, Object> response = new HashMap<>();
        response.put("productId", productId);
        response.put("availableQuantity", available);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/product/{productId}/check")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @PathVariable UUID productId,
            @RequestParam int quantity) {
        boolean available = inventoryService.isAvailable(productId, quantity);
        Map<String, Object> response = new HashMap<>();
        response.put("productId", productId);
        response.put("requestedQuantity", quantity);
        response.put("available", available);
        response.put("availableQuantity", inventoryService.getAvailableQuantity(productId));
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryItem>> getLowStock() {
        return ResponseEntity.ok(inventoryService.findLowStock());
    }
    
    @GetMapping("/reorder")
    public ResponseEntity<List<InventoryItem>> getNeedsReorder() {
        return ResponseEntity.ok(inventoryService.findNeedsReorder());
    }
    
    @PostMapping
    public ResponseEntity<InventoryItem> createInventory(@RequestBody CreateInventoryRequest request) {
        log.info("Creating inventory for product: {}", request.productId());
        InventoryItem item = inventoryService.create(
            request.productId(),
            request.sku(),
            request.quantity(),
            request.warehouseLocation(),
            request.reorderPoint(),
            request.reorderQuantity()
        );
        return ResponseEntity.ok(item);
    }
    
    @PostMapping("/product/{productId}/reserve")
    public ResponseEntity<InventoryItem> reserve(
            @PathVariable UUID productId,
            @RequestParam int quantity) {
        log.info("Reserving {} units for product: {}", quantity, productId);
        return inventoryService.reserve(productId, quantity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/product/{productId}/release")
    public ResponseEntity<InventoryItem> release(
            @PathVariable UUID productId,
            @RequestParam int quantity) {
        log.info("Releasing {} reserved units for product: {}", quantity, productId);
        return inventoryService.releaseReservation(productId, quantity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/product/{productId}/adjust")
    public ResponseEntity<InventoryItem> adjust(
            @PathVariable UUID productId,
            @RequestParam int adjustment) {
        log.info("Adjusting inventory for product {} by: {}", productId, adjustment);
        return inventoryService.adjustQuantity(productId, adjustment)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/product/{productId}/restock")
    public ResponseEntity<InventoryItem> restock(
            @PathVariable UUID productId,
            @RequestParam int quantity) {
        log.info("Restocking product {} with {} units", productId, quantity);
        return inventoryService.restock(productId, quantity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    public record CreateInventoryRequest(
        UUID productId,
        String sku,
        int quantity,
        String warehouseLocation,
        int reorderPoint,
        int reorderQuantity
    ) {}
}
