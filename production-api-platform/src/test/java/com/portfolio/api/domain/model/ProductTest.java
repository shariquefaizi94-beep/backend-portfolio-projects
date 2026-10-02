package com.portfolio.api.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product Domain Model Tests")
class ProductTest {

    @Test
    @DisplayName("Should create product with valid inputs")
    void shouldCreateProductWithValidInputs() {
        Product product = Product.create(
            "Test Product",
            "A test product description",
            "Electronics",
            new BigDecimal("99.99"),
            "USD",
            100
        );

        assertNotNull(product.id());
        assertEquals("Test Product", product.name());
        assertEquals("Electronics", product.category());
        assertEquals(new BigDecimal("99.99"), product.price());
        assertEquals(100, product.stockQuantity());
        assertEquals(ProductStatus.ACTIVE, product.status());
        assertTrue(product.isAvailable());
    }

    @Test
    @DisplayName("Should throw exception for null name")
    void shouldThrowExceptionForNullName() {
        assertThrows(IllegalArgumentException.class, () ->
            Product.create(null, "Description", "Category", BigDecimal.TEN, "USD", 10));
    }

    @Test
    @DisplayName("Should throw exception for blank name")
    void shouldThrowExceptionForBlankName() {
        assertThrows(IllegalArgumentException.class, () ->
            Product.create("  ", "Description", "Category", BigDecimal.TEN, "USD", 10));
    }

    @Test
    @DisplayName("Should throw exception for negative price")
    void shouldThrowExceptionForNegativePrice() {
        assertThrows(IllegalArgumentException.class, () ->
            Product.create("Product", "Description", "Category", new BigDecimal("-10"), "USD", 10));
    }

    @Test
    @DisplayName("Should throw exception for negative stock")
    void shouldThrowExceptionForNegativeStock() {
        assertThrows(IllegalArgumentException.class, () ->
            Product.create("Product", "Description", "Category", BigDecimal.TEN, "USD", -5));
    }

    @Test
    @DisplayName("Should update stock and change status to OUT_OF_STOCK when zero")
    void shouldUpdateStockAndChangeStatus() {
        Product product = Product.create(
            "Test Product", "Description", "Electronics",
            new BigDecimal("50.00"), "USD", 10
        );

        Product updatedProduct = product.withUpdatedStock(0);

        assertEquals(0, updatedProduct.stockQuantity());
        assertEquals(ProductStatus.OUT_OF_STOCK, updatedProduct.status());
        assertFalse(updatedProduct.isAvailable());
    }

    @Test
    @DisplayName("Should update price correctly")
    void shouldUpdatePriceCorrectly() {
        Product product = Product.create(
            "Test Product", "Description", "Electronics",
            new BigDecimal("50.00"), "USD", 10
        );

        Product updatedProduct = product.withUpdatedPrice(new BigDecimal("75.00"));

        assertEquals(new BigDecimal("75.00"), updatedProduct.price());
        assertNotNull(updatedProduct.updatedAt());
    }

    @Test
    @DisplayName("Should maintain immutability when updating")
    void shouldMaintainImmutabilityWhenUpdating() {
        Product original = Product.create(
            "Test Product", "Description", "Electronics",
            new BigDecimal("50.00"), "USD", 10
        );

        Product updated = original.withUpdatedStock(5);

        assertEquals(10, original.stockQuantity());
        assertEquals(5, updated.stockQuantity());
        assertNotSame(original, updated);
    }
}
