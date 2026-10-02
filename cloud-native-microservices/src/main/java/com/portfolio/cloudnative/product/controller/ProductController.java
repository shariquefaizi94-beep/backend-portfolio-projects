package com.portfolio.cloudnative.product.controller;

import com.portfolio.cloudnative.product.model.Product;
import com.portfolio.cloudnative.product.model.ProductStatus;
import com.portfolio.cloudnative.product.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for product operations.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);
    
    private final ProductService productService;
    
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable UUID id) {
        log.debug("Getting product: {}", id);
        return productService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/sku/{sku}")
    public ResponseEntity<Product> getProductBySku(@PathVariable String sku) {
        log.debug("Getting product by SKU: {}", sku);
        return productService.findBySku(sku)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.findAll());
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.findByCategory(category));
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Product>> getProductsByStatus(@PathVariable ProductStatus status) {
        return ResponseEntity.ok(productService.findByStatus(status));
    }
    
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody CreateProductRequest request) {
        log.info("Creating product: {} ({})", request.name(), request.sku());
        Product product = productService.create(
            request.sku(),
            request.name(),
            request.description(),
            request.category(),
            request.price(),
            request.currency()
        );
        return ResponseEntity.ok(product);
    }
    
    @PutMapping("/{id}/price")
    public ResponseEntity<Product> updateProductPrice(
            @PathVariable UUID id,
            @RequestParam BigDecimal price) {
        log.info("Updating product {} price to: {}", id, price);
        return productService.updatePrice(id, price)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<Product> updateProductStatus(
            @PathVariable UUID id,
            @RequestParam ProductStatus status) {
        log.info("Updating product {} status to: {}", id, status);
        return productService.updateStatus(id, status)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        log.info("Deleting product: {}", id);
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    public record CreateProductRequest(
        String sku,
        String name,
        String description,
        String category,
        BigDecimal price,
        String currency
    ) {}
}
