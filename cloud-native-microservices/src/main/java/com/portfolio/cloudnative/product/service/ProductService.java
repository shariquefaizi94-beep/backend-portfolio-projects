package com.portfolio.cloudnative.product.service;

import com.portfolio.cloudnative.product.model.Product;
import com.portfolio.cloudnative.product.model.ProductStatus;
import com.portfolio.cloudnative.product.repository.ProductRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Product service with circuit breaker and retry patterns.
 */
@Service
public class ProductService {
    
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    
    private final ProductRepository productRepository;
    
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    @Timed(value = "product.service.findById")
    @CircuitBreaker(name = "productService", fallbackMethod = "findByIdFallback")
    @Retry(name = "productService")
    public Optional<Product> findById(UUID id) {
        log.debug("Finding product by ID: {}", id);
        return productRepository.findById(id);
    }
    
    public Optional<Product> findByIdFallback(UUID id, Exception e) {
        log.warn("Circuit breaker fallback for findById: {}", e.getMessage());
        return Optional.empty();
    }
    
    @Timed(value = "product.service.findBySku")
    @CircuitBreaker(name = "productService", fallbackMethod = "findBySkuFallback")
    public Optional<Product> findBySku(String sku) {
        log.debug("Finding product by SKU: {}", sku);
        return productRepository.findBySku(sku);
    }
    
    public Optional<Product> findBySkuFallback(String sku, Exception e) {
        log.warn("Circuit breaker fallback for findBySku: {}", e.getMessage());
        return Optional.empty();
    }
    
    @Timed(value = "product.service.findAll")
    public List<Product> findAll() {
        return productRepository.findAll();
    }
    
    @Timed(value = "product.service.findByCategory")
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    
    @Timed(value = "product.service.findByStatus")
    public List<Product> findByStatus(ProductStatus status) {
        return productRepository.findByStatus(status);
    }
    
    @Timed(value = "product.service.create")
    public Product create(String sku, String name, String description, 
                          String category, BigDecimal price, String currency) {
        log.info("Creating product: {} ({})", name, sku);
        
        if (productRepository.existsBySku(sku)) {
            throw new IllegalArgumentException("SKU already exists: " + sku);
        }
        
        Product product = Product.create(sku, name, description, category, price, currency);
        return productRepository.save(product);
    }
    
    @Timed(value = "product.service.updatePrice")
    public Optional<Product> updatePrice(UUID id, BigDecimal newPrice) {
        log.info("Updating product {} price to: {}", id, newPrice);
        return productRepository.findById(id)
            .map(product -> product.withPrice(newPrice))
            .map(productRepository::save);
    }
    
    @Timed(value = "product.service.updateStatus")
    public Optional<Product> updateStatus(UUID id, ProductStatus status) {
        log.info("Updating product {} status to: {}", id, status);
        return productRepository.findById(id)
            .map(product -> product.withStatus(status))
            .map(productRepository::save);
    }
    
    public void delete(UUID id) {
        log.info("Deleting product: {}", id);
        productRepository.deleteById(id);
    }
    
    public long count() {
        return productRepository.count();
    }
}
