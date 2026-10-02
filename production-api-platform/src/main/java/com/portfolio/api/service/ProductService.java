package com.portfolio.api.service;

import com.portfolio.api.domain.model.Product;
import com.portfolio.api.domain.model.ProductStatus;
import com.portfolio.api.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for Product operations with caching support.
 */
@Service
public class ProductService {
    
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    
    private final ProductRepository productRepository;
    
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    @Cacheable(value = "products", key = "#id")
    public Optional<Product> findById(UUID id) {
        log.debug("Finding product by id: {}", id);
        return productRepository.findById(id);
    }
    
    @Cacheable(value = "productsAll", unless = "#result.isEmpty()")
    public List<Product> findAll() {
        log.debug("Finding all products");
        return productRepository.findAll();
    }
    
    @Cacheable(value = "productsByCategory", key = "#category")
    public List<Product> findByCategory(String category) {
        log.debug("Finding products by category: {}", category);
        return productRepository.findByCategory(category);
    }
    
    public List<Product> findAvailable() {
        log.debug("Finding available products");
        return productRepository.findAvailable();
    }
    
    public List<Product> search(String searchTerm) {
        log.debug("Searching products with term: {}", searchTerm);
        return productRepository.findByNameContaining(searchTerm);
    }
    
    public List<Product> findByStatus(ProductStatus status) {
        log.debug("Finding products by status: {}", status);
        return productRepository.findByStatus(status);
    }
    
    @CacheEvict(value = {"products", "productsAll", "productsByCategory"}, allEntries = true)
    public Product create(String name, String description, String category, 
                          BigDecimal price, String currency, int stockQuantity) {
        log.info("Creating new product: {}", name);
        Product product = Product.create(name, description, category, price, currency, stockQuantity);
        return productRepository.save(product);
    }
    
    @CacheEvict(value = {"products", "productsAll", "productsByCategory"}, allEntries = true)
    public Optional<Product> updateStock(UUID id, int newQuantity) {
        log.info("Updating stock for product {}: {}", id, newQuantity);
        return productRepository.findById(id)
            .map(product -> product.withUpdatedStock(newQuantity))
            .map(productRepository::save);
    }
    
    @CacheEvict(value = {"products", "productsAll", "productsByCategory"}, allEntries = true)
    public Optional<Product> updatePrice(UUID id, BigDecimal newPrice) {
        log.info("Updating price for product {}: {}", id, newPrice);
        return productRepository.findById(id)
            .map(product -> product.withUpdatedPrice(newPrice))
            .map(productRepository::save);
    }
    
    @CacheEvict(value = {"products", "productsAll", "productsByCategory"}, allEntries = true)
    public void delete(UUID id) {
        log.info("Deleting product: {}", id);
        productRepository.deleteById(id);
    }
    
    public long count() {
        return productRepository.count();
    }
    
    public List<Product> findPaginated(int page, int size) {
        return productRepository.findAllPaginated(page, size);
    }
}
