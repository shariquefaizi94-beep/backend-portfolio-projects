package com.portfolio.api.gateway.routing;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for API gateway routes.
 * Manages route definitions and provides route matching.
 */
@Component
public class RouteRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(RouteRegistry.class);
    
    private final Map<String, RouteDefinition> routes = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeDefaultRoutes() {
        // Product Service Routes
        registerRoute(RouteDefinition.builder()
            .id("products-all")
            .service("products")
            .pathPattern("/**")
            .methods(Set.of("GET", "POST", "PUT", "DELETE"))
            .backendUrl("http://product-service:8081")
            .enabled(true)
            .priority(1)
            .build());
        
        // Order Service Routes
        registerRoute(RouteDefinition.builder()
            .id("orders-all")
            .service("orders")
            .pathPattern("/**")
            .methods(Set.of("GET", "POST", "PUT"))
            .backendUrl("http://order-service:8082")
            .enabled(true)
            .authRequired(true)
            .priority(1)
            .build());
        
        // User Service Routes
        registerRoute(RouteDefinition.builder()
            .id("users-all")
            .service("users")
            .pathPattern("/**")
            .methods(Set.of("GET", "POST", "PUT", "DELETE"))
            .backendUrl("http://user-service:8083")
            .enabled(true)
            .authRequired(true)
            .priority(1)
            .build());
        
        // Analytics Service Routes (read-only)
        registerRoute(RouteDefinition.builder()
            .id("analytics-all")
            .service("analytics")
            .pathPattern("/**")
            .methods(Set.of("GET"))
            .backendUrl("http://analytics-service:8084")
            .enabled(true)
            .priority(1)
            .build());
        
        // Search Service Routes
        registerRoute(RouteDefinition.builder()
            .id("search-all")
            .service("search")
            .pathPattern("/**")
            .methods(Set.of("GET", "POST"))
            .backendUrl("http://search-service:8085")
            .enabled(true)
            .timeoutMs(5000)
            .build());
        
        log.info("Initialized {} default routes", routes.size());
    }
    
    public void registerRoute(RouteDefinition route) {
        routes.put(route.getId(), route);
        log.info("Registered route: {} -> {}", route.getId(), route.getBackendUrl());
    }
    
    public void unregisterRoute(String routeId) {
        RouteDefinition removed = routes.remove(routeId);
        if (removed != null) {
            log.info("Unregistered route: {}", routeId);
        }
    }
    
    public Optional<RouteDefinition> findRoute(String service, String path, String method) {
        return routes.values().stream()
            .filter(route -> route.matches(service, path, method))
            .max(Comparator.comparingInt(RouteDefinition::getPriority));
    }
    
    public Optional<RouteDefinition> getRoute(String routeId) {
        return Optional.ofNullable(routes.get(routeId));
    }
    
    public List<RouteDefinition> getAllRoutes() {
        return new ArrayList<>(routes.values());
    }
    
    public List<RouteDefinition> getRoutesByService(String service) {
        return routes.values().stream()
            .filter(route -> route.getService().equals(service))
            .toList();
    }
    
    public int getRouteCount() {
        return routes.size();
    }
    
    public void enableRoute(String routeId) {
        getRoute(routeId).ifPresent(route -> {
            RouteDefinition enabled = RouteDefinition.builder()
                .id(route.getId())
                .service(route.getService())
                .pathPattern(route.getPathPattern())
                .methods(route.getMethods())
                .backendUrl(route.getBackendUrl())
                .enabled(true)
                .priority(route.getPriority())
                .headerTransformations(route.getHeaderTransformations())
                .requiredHeaders(route.getRequiredHeaders())
                .authRequired(route.isAuthRequired())
                .timeoutMs(route.getTimeoutMs())
                .retryCount(route.getRetryCount())
                .build();
            routes.put(routeId, enabled);
            log.info("Enabled route: {}", routeId);
        });
    }
    
    public void disableRoute(String routeId) {
        getRoute(routeId).ifPresent(route -> {
            RouteDefinition disabled = RouteDefinition.builder()
                .id(route.getId())
                .service(route.getService())
                .pathPattern(route.getPathPattern())
                .methods(route.getMethods())
                .backendUrl(route.getBackendUrl())
                .enabled(false)
                .priority(route.getPriority())
                .headerTransformations(route.getHeaderTransformations())
                .requiredHeaders(route.getRequiredHeaders())
                .authRequired(route.isAuthRequired())
                .timeoutMs(route.getTimeoutMs())
                .retryCount(route.getRetryCount())
                .build();
            routes.put(routeId, disabled);
            log.info("Disabled route: {}", routeId);
        });
    }
}
