package com.portfolio.api.graphql;

import com.portfolio.api.domain.model.Product;
import com.portfolio.api.domain.model.ProductStatus;
import com.portfolio.api.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GraphQL resolver for Product operations.
 */
@Controller
public class ProductResolver {
    
    private static final Logger log = LoggerFactory.getLogger(ProductResolver.class);
    
    private final ProductService productService;
    
    public ProductResolver(ProductService productService) {
        this.productService = productService;
    }
    
    // ============================================
    // Queries
    // ============================================
    
    @QueryMapping
    public Optional<Product> product(@Argument UUID id) {
        log.debug("GraphQL query: product(id={})", id);
        return productService.findById(id);
    }
    
    @QueryMapping
    public ProductConnection products(
            @Argument ProductFilter filter,
            @Argument Integer page,
            @Argument Integer size) {
        
        log.debug("GraphQL query: products(filter={}, page={}, size={})", filter, page, size);
        
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? Math.min(size, 100) : 10;
        
        List<Product> allProducts = productService.findAll();
        List<Product> filtered = applyFilter(allProducts, filter);
        
        // Calculate pagination
        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int start = pageNum * pageSize;
        int end = Math.min(start + pageSize, totalElements);
        
        List<Product> pageProducts = start < totalElements 
            ? filtered.subList(start, end) 
            : List.of();
        
        PageInfo pageInfo = new PageInfo(
            totalElements,
            totalPages,
            pageNum,
            pageSize,
            pageNum < totalPages - 1,
            pageNum > 0
        );
        
        return new ProductConnection(pageProducts, pageInfo);
    }
    
    @QueryMapping
    public List<Product> searchProducts(@Argument String searchTerm, @Argument Integer limit) {
        log.debug("GraphQL query: searchProducts(term={}, limit={})", searchTerm, limit);
        List<Product> results = productService.search(searchTerm);
        if (limit != null && limit > 0) {
            return results.stream().limit(limit).collect(Collectors.toList());
        }
        return results;
    }
    
    @QueryMapping
    public ProductStats productStats() {
        log.debug("GraphQL query: productStats");
        List<Product> all = productService.findAll();
        
        long active = all.stream().filter(p -> p.status() == ProductStatus.ACTIVE).count();
        long outOfStock = all.stream().filter(p -> p.status() == ProductStatus.OUT_OF_STOCK).count();
        
        Map<String, Long> categoryMap = all.stream()
            .collect(Collectors.groupingBy(Product::category, Collectors.counting()));
        
        List<CategoryCount> categoryCounts = categoryMap.entrySet().stream()
            .map(e -> new CategoryCount(e.getKey(), e.getValue().intValue()))
            .collect(Collectors.toList());
        
        return new ProductStats((int) all.size(), (int) active, (int) outOfStock, categoryCounts);
    }
    
    // ============================================
    // Mutations
    // ============================================
    
    @MutationMapping
    public Product createProduct(@Argument CreateProductInput input) {
        log.info("GraphQL mutation: createProduct(name={})", input.name());
        return productService.create(
            input.name(),
            input.description(),
            input.category(),
            input.price(),
            input.currency() != null ? input.currency() : "USD",
            input.stockQuantity()
        );
    }
    
    @MutationMapping
    public Optional<Product> updateProductStock(@Argument UUID id, @Argument int quantity) {
        log.info("GraphQL mutation: updateProductStock(id={}, quantity={})", id, quantity);
        return productService.updateStock(id, quantity);
    }
    
    @MutationMapping
    public Optional<Product> updateProductPrice(@Argument UUID id, @Argument BigDecimal price) {
        log.info("GraphQL mutation: updateProductPrice(id={}, price={})", id, price);
        return productService.updatePrice(id, price);
    }
    
    @MutationMapping
    public boolean deleteProduct(@Argument UUID id) {
        log.info("GraphQL mutation: deleteProduct(id={})", id);
        productService.delete(id);
        return true;
    }
    
    // ============================================
    // Helper methods
    // ============================================
    
    private List<Product> applyFilter(List<Product> products, ProductFilter filter) {
        if (filter == null) {
            return products;
        }
        
        return products.stream()
            .filter(p -> filter.category() == null || 
                        p.category().equalsIgnoreCase(filter.category()))
            .filter(p -> filter.status() == null || 
                        p.status() == filter.status())
            .filter(p -> filter.minPrice() == null || 
                        p.price().compareTo(filter.minPrice()) >= 0)
            .filter(p -> filter.maxPrice() == null || 
                        p.price().compareTo(filter.maxPrice()) <= 0)
            .filter(p -> filter.available() == null || 
                        p.isAvailable() == filter.available())
            .collect(Collectors.toList());
    }
    
    // ============================================
    // DTOs
    // ============================================
    
    public record ProductFilter(
        String category,
        ProductStatus status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean available
    ) {}
    
    public record CreateProductInput(
        String name,
        String description,
        String category,
        BigDecimal price,
        String currency,
        int stockQuantity
    ) {}
    
    public record ProductConnection(
        List<Product> products,
        PageInfo pageInfo
    ) {}
    
    public record PageInfo(
        int totalElements,
        int totalPages,
        int currentPage,
        int pageSize,
        boolean hasNext,
        boolean hasPrevious
    ) {}
    
    public record ProductStats(
        int totalProducts,
        int activeProducts,
        int outOfStockProducts,
        List<CategoryCount> categoryCounts
    ) {}
    
    public record CategoryCount(
        String category,
        int count
    ) {}
}
