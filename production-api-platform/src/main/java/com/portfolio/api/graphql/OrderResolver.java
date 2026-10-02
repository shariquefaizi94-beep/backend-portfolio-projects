package com.portfolio.api.graphql;

import com.portfolio.api.domain.model.Order;
import com.portfolio.api.domain.model.OrderStatus;
import com.portfolio.api.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GraphQL resolver for Order operations.
 */
@Controller
public class OrderResolver {
    
    private static final Logger log = LoggerFactory.getLogger(OrderResolver.class);
    
    private final OrderService orderService;
    
    public OrderResolver(OrderService orderService) {
        this.orderService = orderService;
    }
    
    // ============================================
    // Queries
    // ============================================
    
    @QueryMapping
    public Optional<Order> order(@Argument UUID id) {
        log.debug("GraphQL query: order(id={})", id);
        return orderService.findById(id);
    }
    
    @QueryMapping
    public OrderConnection orders(
            @Argument OrderFilter filter,
            @Argument Integer page,
            @Argument Integer size) {
        
        log.debug("GraphQL query: orders(filter={}, page={}, size={})", filter, page, size);
        
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? Math.min(size, 100) : 10;
        
        List<Order> allOrders;
        if (filter != null && filter.userId() != null) {
            if (filter.status() != null) {
                allOrders = orderService.findByUserIdAndStatus(filter.userId(), filter.status());
            } else {
                allOrders = orderService.findByUserId(filter.userId());
            }
        } else if (filter != null && filter.status() != null) {
            allOrders = orderService.findByStatus(filter.status());
        } else {
            allOrders = orderService.findPaginated(0, 1000); // Get all for filtering
        }
        
        // Calculate pagination
        int totalElements = allOrders.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int start = pageNum * pageSize;
        int end = Math.min(start + pageSize, totalElements);
        
        List<Order> pageOrders = start < totalElements 
            ? allOrders.subList(start, end) 
            : List.of();
        
        PageInfo pageInfo = new PageInfo(
            totalElements,
            totalPages,
            pageNum,
            pageSize,
            pageNum < totalPages - 1,
            pageNum > 0
        );
        
        return new OrderConnection(pageOrders, pageInfo);
    }
    
    @QueryMapping
    public List<Order> ordersByUser(@Argument UUID userId) {
        log.debug("GraphQL query: ordersByUser(userId={})", userId);
        return orderService.findByUserId(userId);
    }
    
    @QueryMapping
    public OrderStats orderStats() {
        log.debug("GraphQL query: orderStats");
        
        long total = orderService.count();
        long pending = orderService.countByStatus(OrderStatus.PENDING);
        long completed = orderService.countByStatus(OrderStatus.DELIVERED);
        long cancelled = orderService.countByStatus(OrderStatus.CANCELLED);
        
        // Calculate total revenue from delivered orders
        BigDecimal revenue = orderService.findByStatus(OrderStatus.DELIVERED).stream()
            .map(Order::total)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new OrderStats((int) total, (int) pending, (int) completed, (int) cancelled, revenue);
    }
    
    // ============================================
    // Mutations
    // ============================================
    
    @MutationMapping
    public Order createOrder(@Argument CreateOrderInput input) {
        log.info("GraphQL mutation: createOrder(userId={}, items={})", 
                input.userId(), input.items().size());
        
        List<OrderService.OrderItemInput> items = input.items().stream()
            .map(item -> new OrderService.OrderItemInput(item.productId(), item.quantity()))
            .collect(Collectors.toList());
        
        return orderService.createOrder(
            input.userId(),
            items,
            input.shippingAddress(),
            input.billingAddress()
        );
    }
    
    @MutationMapping
    public Optional<Order> updateOrderStatus(@Argument UUID id, @Argument OrderStatus status) {
        log.info("GraphQL mutation: updateOrderStatus(id={}, status={})", id, status);
        return orderService.updateStatus(id, status);
    }
    
    @MutationMapping
    public Optional<Order> cancelOrder(@Argument UUID id) {
        log.info("GraphQL mutation: cancelOrder(id={})", id);
        return orderService.cancelOrder(id);
    }
    
    // ============================================
    // DTOs
    // ============================================
    
    public record OrderFilter(
        UUID userId,
        OrderStatus status,
        Instant startDate,
        Instant endDate
    ) {}
    
    public record CreateOrderInput(
        UUID userId,
        List<OrderItemInput> items,
        String shippingAddress,
        String billingAddress
    ) {}
    
    public record OrderItemInput(
        UUID productId,
        int quantity
    ) {}
    
    public record OrderConnection(
        List<Order> orders,
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
    
    public record OrderStats(
        int totalOrders,
        int pendingOrders,
        int completedOrders,
        int cancelledOrders,
        BigDecimal totalRevenue
    ) {}
}
