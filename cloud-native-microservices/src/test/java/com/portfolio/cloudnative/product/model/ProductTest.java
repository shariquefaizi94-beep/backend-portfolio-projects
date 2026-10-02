package com.portfolio.cloudnative.product.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product Domain Tests")
class ProductTest {
    
    @Test
    @DisplayName("Should create product with valid inputs")
    void shouldCreateProduct() {
        Product product = Product.create("SKU-001", "Laptop", "A laptop", "Electronics", 
            new BigDecimal("999.99"), "USD");
        
        assertNotNull(product.id());
        assertEquals("SKU-001", product.sku());
        assertEquals("Laptop", product.name());
        assertEquals("Electronics", product.category());
        assertEquals(new BigDecimal("999.99"), product.price());
        assertEquals(ProductStatus.ACTIVE, product.status());
    }
    
    @Test
    @DisplayName("Should throw exception for null SKU")
    void shouldThrowForNullSku() {
        assertThrows(IllegalArgumentException.class, () ->
            Product.create(null, "Name", "Desc", "Cat", BigDecimal.TEN, "USD"));
    }
    
    @Test
    @DisplayName("Should throw exception for blank name")
    void shouldThrowForBlankName() {
        assertThrows(IllegalArgumentException.class, () ->
            Product.create("SKU", "  ", "Desc", "Cat", BigDecimal.TEN, "USD"));
    }
    
    @Test
    @DisplayName("Should throw exception for negative price")
    void shouldThrowForNegativePrice() {
        assertThrows(IllegalArgumentException.class, () ->
            Product.create("SKU", "Name", "Desc", "Cat", new BigDecimal("-10"), "USD"));
    }
    
    @Test
    @DisplayName("Should update price immutably")
    void shouldUpdatePrice() {
        Product original = Product.create("SKU-001", "Laptop", "Desc", "Electronics", 
            new BigDecimal("999.99"), "USD");
        
        Product updated = original.withPrice(new BigDecimal("1099.99"));
        
        assertEquals(new BigDecimal("999.99"), original.price());
        assertEquals(new BigDecimal("1099.99"), updated.price());
        assertNotSame(original, updated);
    }
    
    @Test
    @DisplayName("Should update status immutably")
    void shouldUpdateStatus() {
        Product original = Product.create("SKU-001", "Laptop", "Desc", "Electronics", 
            new BigDecimal("999.99"), "USD");
        
        Product updated = original.withStatus(ProductStatus.DISCONTINUED);
        
        assertEquals(ProductStatus.ACTIVE, original.status());
        assertEquals(ProductStatus.DISCONTINUED, updated.status());
    }
    
    @Test
    @DisplayName("Should default currency to USD")
    void shouldDefaultCurrencyToUsd() {
        Product product = Product.create("SKU-001", "Laptop", "Desc", "Electronics", 
            new BigDecimal("999.99"), null);
        
        assertEquals("USD", product.currency());
    }
}
