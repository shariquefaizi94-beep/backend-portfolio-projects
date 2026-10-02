package com.portfolio.api.service;

import com.portfolio.api.domain.model.Product;
import com.portfolio.api.domain.model.ProductStatus;
import com.portfolio.api.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Product Service Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    @DisplayName("Should find product by ID")
    void shouldFindProductById() {
        UUID productId = UUID.randomUUID();
        Product product = createProduct(productId, "Test Product");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Optional<Product> result = productService.findById(productId);

        assertTrue(result.isPresent());
        assertEquals("Test Product", result.get().name());
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("Should return empty when product not found")
    void shouldReturnEmptyWhenProductNotFound() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        Optional<Product> result = productService.findById(productId);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should find all products")
    void shouldFindAllProducts() {
        List<Product> products = List.of(
            createProduct(UUID.randomUUID(), "Product 1"),
            createProduct(UUID.randomUUID(), "Product 2")
        );
        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = productService.findAll();

        assertEquals(2, result.size());
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should find products by category")
    void shouldFindProductsByCategory() {
        String category = "Electronics";
        List<Product> products = List.of(
            createProduct(UUID.randomUUID(), "Phone"),
            createProduct(UUID.randomUUID(), "Laptop")
        );
        when(productRepository.findByCategory(category)).thenReturn(products);

        List<Product> result = productService.findByCategory(category);

        assertEquals(2, result.size());
        verify(productRepository).findByCategory(category);
    }

    @Test
    @DisplayName("Should create product")
    void shouldCreateProduct() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.create(
            "New Product",
            "Description",
            "Electronics",
            new BigDecimal("99.99"),
            "USD",
            50
        );

        assertNotNull(result);
        assertEquals("New Product", result.name());
        assertEquals("Electronics", result.category());
        assertEquals(new BigDecimal("99.99"), result.price());
        assertEquals(50, result.stockQuantity());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should update product stock")
    void shouldUpdateProductStock() {
        UUID productId = UUID.randomUUID();
        Product product = createProduct(productId, "Test Product");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Product> result = productService.updateStock(productId, 25);

        assertTrue(result.isPresent());
        assertEquals(25, result.get().stockQuantity());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should update product price")
    void shouldUpdateProductPrice() {
        UUID productId = UUID.randomUUID();
        Product product = createProduct(productId, "Test Product");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Product> result = productService.updatePrice(productId, new BigDecimal("149.99"));

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("149.99"), result.get().price());
    }

    @Test
    @DisplayName("Should delete product")
    void shouldDeleteProduct() {
        UUID productId = UUID.randomUUID();
        doNothing().when(productRepository).deleteById(productId);

        productService.delete(productId);

        verify(productRepository).deleteById(productId);
    }

    @Test
    @DisplayName("Should search products by name")
    void shouldSearchProductsByName() {
        List<Product> products = List.of(
            createProduct(UUID.randomUUID(), "iPhone 15"),
            createProduct(UUID.randomUUID(), "iPhone 14")
        );
        when(productRepository.findByNameContaining("iPhone")).thenReturn(products);

        List<Product> result = productService.search("iPhone");

        assertEquals(2, result.size());
        verify(productRepository).findByNameContaining("iPhone");
    }

    private Product createProduct(UUID id, String name) {
        Instant now = Instant.now();
        return new Product(
            id,
            name,
            "Test description",
            "Electronics",
            new BigDecimal("99.99"),
            "USD",
            100,
            ProductStatus.ACTIVE,
            now,
            now
        );
    }
}
