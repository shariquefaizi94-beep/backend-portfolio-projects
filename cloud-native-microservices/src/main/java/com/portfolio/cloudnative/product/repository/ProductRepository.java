package com.portfolio.cloudnative.product.repository;

import com.portfolio.cloudnative.product.model.Product;
import com.portfolio.cloudnative.product.model.ProductStatus;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for products with sample data.
 */
@Repository
public class ProductRepository {
    
    private final Map<UUID, Product> products = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeSampleData() {
        saveProduct("SKU-001", "Laptop Pro 15", "High-performance laptop", "Electronics", new BigDecimal("1299.99"));
        saveProduct("SKU-002", "Wireless Mouse", "Ergonomic wireless mouse", "Electronics", new BigDecimal("49.99"));
        saveProduct("SKU-003", "USB-C Hub", "7-in-1 USB-C hub", "Electronics", new BigDecimal("79.99"));
        saveProduct("SKU-004", "Monitor Stand", "Adjustable monitor stand", "Office", new BigDecimal("129.99"));
        saveProduct("SKU-005", "Desk Mat", "Large desk mat", "Office", new BigDecimal("29.99"));
    }
    
    private void saveProduct(String sku, String name, String description, String category, BigDecimal price) {
        Product product = Product.create(sku, name, description, category, price, "USD");
        products.put(product.id(), product);
    }
    
    public Optional<Product> findById(UUID id) {
        return Optional.ofNullable(products.get(id));
    }
    
    public Optional<Product> findBySku(String sku) {
        return products.values().stream()
            .filter(p -> p.sku().equalsIgnoreCase(sku))
            .findFirst();
    }
    
    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }
    
    public List<Product> findByCategory(String category) {
        return products.values().stream()
            .filter(p -> p.category().equalsIgnoreCase(category))
            .collect(Collectors.toList());
    }
    
    public List<Product> findByStatus(ProductStatus status) {
        return products.values().stream()
            .filter(p -> p.status() == status)
            .collect(Collectors.toList());
    }
    
    public Product save(Product product) {
        products.put(product.id(), product);
        return product;
    }
    
    public void deleteById(UUID id) {
        products.remove(id);
    }
    
    public boolean existsBySku(String sku) {
        return products.values().stream()
            .anyMatch(p -> p.sku().equalsIgnoreCase(sku));
    }
    
    public long count() {
        return products.size();
    }
}
