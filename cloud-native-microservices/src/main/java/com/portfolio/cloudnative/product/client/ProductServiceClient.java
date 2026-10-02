package com.portfolio.cloudnative.product.client;

import com.portfolio.cloudnative.product.model.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for product service.
 * Demonstrates service-to-service communication in microservices.
 * 
 * In production, this would call the actual product-service via Kubernetes service discovery
 * or through a service mesh like Istio.
 */
@FeignClient(
    name = "product-service",
    url = "${services.product.url:http://localhost:8081}",
    fallback = ProductServiceClientFallback.class
)
public interface ProductServiceClient {
    
    @GetMapping("/api/products/{id}")
    Product getProduct(@PathVariable("id") UUID id);
    
    @GetMapping("/api/products")
    List<Product> getAllProducts();
    
    @GetMapping("/api/products/sku/{sku}")
    Product getProductBySku(@PathVariable("sku") String sku);
    
    @GetMapping("/api/products/category/{category}")
    List<Product> getProductsByCategory(@PathVariable("category") String category);
}
