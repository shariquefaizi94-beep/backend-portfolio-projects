package com.portfolio.api.repository;

import com.portfolio.api.domain.model.Product;
import com.portfolio.api.domain.model.ProductStatus;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of ProductRepository.
 * Provides thread-safe operations with sample data for demonstration.
 */
@Repository
public class InMemoryProductRepository implements ProductRepository {
    
    private final Map<UUID, Product> products = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeSampleData() {
        // Electronics
        saveProduct("MacBook Pro 16\"", "Apple M3 Max chip, 36GB RAM, 1TB SSD", 
                    "Electronics", new BigDecimal("3499.99"), 50);
        saveProduct("iPhone 15 Pro", "A17 Pro chip, 256GB, Natural Titanium", 
                    "Electronics", new BigDecimal("1199.99"), 200);
        saveProduct("Samsung Galaxy S24 Ultra", "Snapdragon 8 Gen 3, 512GB", 
                    "Electronics", new BigDecimal("1299.99"), 150);
        saveProduct("Sony WH-1000XM5", "Wireless Noise Canceling Headphones", 
                    "Electronics", new BigDecimal("399.99"), 100);
        
        // Books
        saveProduct("Clean Code", "Robert C. Martin - Software Craftsmanship", 
                    "Books", new BigDecimal("44.99"), 500);
        saveProduct("System Design Interview", "Alex Xu - Volume 1", 
                    "Books", new BigDecimal("39.99"), 300);
        saveProduct("Designing Data-Intensive Applications", "Martin Kleppmann", 
                    "Books", new BigDecimal("54.99"), 250);
        
        // Clothing
        saveProduct("Nike Air Jordan 1", "Retro High OG - Chicago", 
                    "Clothing", new BigDecimal("180.00"), 75);
        saveProduct("Levi's 501 Original", "Classic Straight Fit Jeans", 
                    "Clothing", new BigDecimal("69.99"), 400);
    }
    
    private void saveProduct(String name, String description, String category, 
                              BigDecimal price, int stock) {
        Product product = Product.create(name, description, category, price, "USD", stock);
        products.put(product.id(), product);
    }
    
    @Override
    public Optional<Product> findById(UUID id) {
        return Optional.ofNullable(products.get(id));
    }
    
    @Override
    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }
    
    @Override
    public List<Product> findByCategory(String category) {
        return products.values().stream()
            .filter(p -> p.category().equalsIgnoreCase(category))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Product> findByStatus(ProductStatus status) {
        return products.values().stream()
            .filter(p -> p.status() == status)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Product> findByNameContaining(String searchTerm) {
        String lowerSearch = searchTerm.toLowerCase();
        return products.values().stream()
            .filter(p -> p.name().toLowerCase().contains(lowerSearch))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Product> findAvailable() {
        return products.values().stream()
            .filter(Product::isAvailable)
            .collect(Collectors.toList());
    }
    
    @Override
    public Product save(Product product) {
        products.put(product.id(), product);
        return product;
    }
    
    @Override
    public void deleteById(UUID id) {
        products.remove(id);
    }
    
    @Override
    public boolean existsById(UUID id) {
        return products.containsKey(id);
    }
    
    @Override
    public long count() {
        return products.size();
    }
    
    @Override
    public List<Product> findAllPaginated(int page, int size) {
        return products.values().stream()
            .skip((long) page * size)
            .limit(size)
            .collect(Collectors.toList());
    }
}
