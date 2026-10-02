package com.portfolio.cloudnative.inventory.service;

import com.portfolio.cloudnative.inventory.model.InventoryItem;
import com.portfolio.cloudnative.inventory.model.InventoryStatus;
import com.portfolio.cloudnative.inventory.repository.InventoryRepository;
import com.portfolio.cloudnative.notification.client.NotificationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Inventory Service Tests")
class InventoryServiceTest {
    
    @Mock
    private InventoryRepository inventoryRepository;
    
    @Mock
    private NotificationClient notificationClient;
    
    private InventoryService inventoryService;
    
    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, notificationClient);
    }
    
    @Test
    @DisplayName("Should find inventory by ID")
    void shouldFindById() {
        UUID inventoryId = UUID.randomUUID();
        InventoryItem item = InventoryItem.create(UUID.randomUUID(), "SKU-001", 100, "WH-A", 10, 50);
        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(item));
        
        Optional<InventoryItem> result = inventoryService.findById(inventoryId);
        
        assertTrue(result.isPresent());
        assertEquals("SKU-001", result.get().sku());
    }
    
    @Test
    @DisplayName("Should find inventory by product ID")
    void shouldFindByProductId() {
        UUID productId = UUID.randomUUID();
        InventoryItem item = InventoryItem.create(productId, "SKU-001", 100, "WH-A", 10, 50);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(item));
        
        Optional<InventoryItem> result = inventoryService.findByProductId(productId);
        
        assertTrue(result.isPresent());
        assertEquals(productId, result.get().productId());
    }
    
    @Test
    @DisplayName("Should create inventory item")
    void shouldCreateInventory() {
        UUID productId = UUID.randomUUID();
        when(inventoryRepository.existsByProductId(productId)).thenReturn(false);
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        InventoryItem result = inventoryService.create(productId, "SKU-001", 100, "WH-A", 10, 50);
        
        assertNotNull(result);
        assertEquals(productId, result.productId());
        assertEquals(100, result.quantityOnHand());
        verify(inventoryRepository).save(any(InventoryItem.class));
    }
    
    @Test
    @DisplayName("Should throw when inventory already exists")
    void shouldThrowWhenInventoryExists() {
        UUID productId = UUID.randomUUID();
        when(inventoryRepository.existsByProductId(productId)).thenReturn(true);
        
        assertThrows(IllegalArgumentException.class, () ->
            inventoryService.create(productId, "SKU", 100, "WH", 10, 50));
    }
    
    @Test
    @DisplayName("Should reserve inventory")
    void shouldReserveInventory() {
        UUID productId = UUID.randomUUID();
        InventoryItem item = InventoryItem.create(productId, "SKU-001", 100, "WH-A", 10, 50);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Optional<InventoryItem> result = inventoryService.reserve(productId, 30);
        
        assertTrue(result.isPresent());
        assertEquals(30, result.get().quantityReserved());
        assertEquals(70, result.get().availableQuantity());
    }
    
    @Test
    @DisplayName("Should throw when reserving insufficient stock")
    void shouldThrowWhenInsufficientStock() {
        UUID productId = UUID.randomUUID();
        InventoryItem item = InventoryItem.create(productId, "SKU-001", 50, "WH-A", 10, 50);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(item));
        
        assertThrows(IllegalStateException.class, () ->
            inventoryService.reserve(productId, 100));
    }
    
    @Test
    @DisplayName("Should release reservation")
    void shouldReleaseReservation() {
        UUID productId = UUID.randomUUID();
        InventoryItem item = InventoryItem.create(productId, "SKU-001", 100, "WH-A", 10, 50)
            .reserve(30);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Optional<InventoryItem> result = inventoryService.releaseReservation(productId, 20);
        
        assertTrue(result.isPresent());
        assertEquals(10, result.get().quantityReserved());
    }
    
    @Test
    @DisplayName("Should check availability correctly")
    void shouldCheckAvailability() {
        UUID productId = UUID.randomUUID();
        InventoryItem item = InventoryItem.create(productId, "SKU-001", 100, "WH-A", 10, 50);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(item));
        
        assertTrue(inventoryService.isAvailable(productId, 50));
        assertTrue(inventoryService.isAvailable(productId, 100));
        assertFalse(inventoryService.isAvailable(productId, 150));
    }
    
    @Test
    @DisplayName("Should get available quantity")
    void shouldGetAvailableQuantity() {
        UUID productId = UUID.randomUUID();
        InventoryItem item = InventoryItem.create(productId, "SKU-001", 100, "WH-A", 10, 50)
            .reserve(25);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(item));
        
        int available = inventoryService.getAvailableQuantity(productId);
        
        assertEquals(75, available);
    }
}
