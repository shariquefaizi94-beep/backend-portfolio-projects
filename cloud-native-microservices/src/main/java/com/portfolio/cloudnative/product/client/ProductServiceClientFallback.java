package com.portfolio.cloudnative.product.client;

import com.portfolio.cloudnative.product.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Fallback implementation for ProductServiceClient.
 * Provides graceful degradation when product service is unavailable.
 */
@Component
public class ProductServiceClientFallback implements ProductServiceClient {
    
    private static final Logger log = LoggerFactory.getLogger(ProductServiceClientFallback.class);
    
    @Override
    public Product getProduct(UUID id) {
        log.warn("Product service unavailable. Fallback for getProduct: {}", id);
        return null;
    }
    
    @Override
    public List<Product> getAllProducts() {
        log.warn("Product service unavailable. Fallback for getAllProducts");
        return Collections.emptyList();
    }
    
    @Override
    public Product getProductBySku(String sku) {
        log.warn("Product service unavailable. Fallback for getProductBySku: {}", sku);
        return null;
    }
    
    @Override
    public List<Product> getProductsByCategory(String category) {
        log.warn("Product service unavailable. Fallback for getProductsByCategory: {}", category);
        return Collections.emptyList();
    }
}
