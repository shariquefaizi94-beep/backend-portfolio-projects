package com.portfolio.cloudnative.inventory.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Inventory Item Domain Tests")
class InventoryItemTest {
    
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    
    @Test
    @DisplayName("Should create inventory item with valid inputs")
    void shouldCreateInventoryItem() {
        InventoryItem item = InventoryItem.create(PRODUCT_ID, "SKU-001", 100, "Warehouse-A", 10, 50);
        
        assertNotNull(item.id());
        assertEquals(PRODUCT_ID, item.productId());
        assertEquals("SKU-001", item.sku());
        assertEquals(100, item.quantityOnHand());
        assertEquals(0, item.quantityReserved());
        assertEquals(100, item.availableQuantity());
        assertEquals(InventoryStatus.IN_STOCK, item.status());
    }
    
    @Test
    @DisplayName("Should throw exception for null product ID")
    void shouldThrowForNullProductId() {
        assertThrows(IllegalArgumentException.class, () ->
            InventoryItem.create(null, "SKU", 10, "WH", 5, 20));
    }
    
    @Test
    @DisplayName("Should throw exception for negative quantity")
    void shouldThrowForNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () ->
            InventoryItem.create(PRODUCT_ID, "SKU", -5, "WH", 5, 20));
    }
    
    @Test
    @DisplayName("Should reserve quantity correctly")
    void shouldReserveQuantity() {
        InventoryItem item = InventoryItem.create(PRODUCT_ID, "SKU-001", 100, "WH-A", 10, 50);
        
        InventoryItem reserved = item.reserve(30);
        
        assertEquals(100, reserved.quantityOnHand());
        assertEquals(30, reserved.quantityReserved());
        assertEquals(70, reserved.availableQuantity());
    }
    
    @Test
    @DisplayName("Should throw exception when reserving more than available")
    void shouldThrowWhenReservingMoreThanAvailable() {
        InventoryItem item = InventoryItem.create(PRODUCT_ID, "SKU-001", 50, "WH-A", 10, 50);
        
        assertThrows(IllegalStateException.class, () -> item.reserve(60));
    }
    
    @Test
    @DisplayName("Should release reservation correctly")
    void shouldReleaseReservation() {
        InventoryItem item = InventoryItem.create(PRODUCT_ID, "SKU-001", 100, "WH-A", 10, 50)
            .reserve(30);
        
        InventoryItem released = item.releaseReservation(20);
        
        assertEquals(10, released.quantityReserved());
        assertEquals(90, released.availableQuantity());
    }
    
    @Test
    @DisplayName("Should adjust quantity and update status")
    void shouldAdjustQuantityAndUpdateStatus() {
        InventoryItem item = InventoryItem.create(PRODUCT_ID, "SKU-001", 100, "WH-A", 20, 50);
        
        // Decrease to low stock
        InventoryItem lowStock = item.adjustQuantity(-90);
        assertEquals(10, lowStock.quantityOnHand());
        assertEquals(InventoryStatus.LOW_STOCK, lowStock.status());
        
        // Decrease to out of stock
        InventoryItem outOfStock = lowStock.adjustQuantity(-10);
        assertEquals(0, outOfStock.quantityOnHand());
        assertEquals(InventoryStatus.OUT_OF_STOCK, outOfStock.status());
    }
    
    @Test
    @DisplayName("Should detect when reorder is needed")
    void shouldDetectReorderNeeded() {
        InventoryItem highStock = InventoryItem.create(PRODUCT_ID, "SKU-001", 100, "WH-A", 20, 50);
        assertFalse(highStock.needsReorder());
        
        InventoryItem lowStock = highStock.adjustQuantity(-85);
        assertTrue(lowStock.needsReorder());
    }
    
    @Test
    @DisplayName("Should calculate available quantity correctly")
    void shouldCalculateAvailableQuantity() {
        InventoryItem item = InventoryItem.create(PRODUCT_ID, "SKU-001", 100, "WH-A", 10, 50)
            .reserve(25)
            .reserve(15);
        
        assertEquals(60, item.availableQuantity());
    }
}
