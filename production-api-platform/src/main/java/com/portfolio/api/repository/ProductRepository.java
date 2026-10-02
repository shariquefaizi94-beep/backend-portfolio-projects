package com.portfolio.api.repository;

import com.portfolio.api.domain.model.Product;
import com.portfolio.api.domain.model.ProductStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Product persistence operations.
 */
public interface ProductRepository {
    
    Optional<Product> findById(UUID id);
    
    List<Product> findAll();
    
    List<Product> findByCategory(String category);
    
    List<Product> findByStatus(ProductStatus status);
    
    List<Product> findByNameContaining(String searchTerm);
    
    List<Product> findAvailable();
    
    Product save(Product product);
    
    void deleteById(UUID id);
    
    boolean existsById(UUID id);
    
    long count();
    
    List<Product> findAllPaginated(int page, int size);
}
