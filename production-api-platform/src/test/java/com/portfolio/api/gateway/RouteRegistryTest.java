package com.portfolio.api.gateway;

import com.portfolio.api.gateway.routing.RouteDefinition;
import com.portfolio.api.gateway.routing.RouteRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Route Registry Tests")
class RouteRegistryTest {

    private RouteRegistry routeRegistry;

    @BeforeEach
    void setUp() {
        routeRegistry = new RouteRegistry();
        routeRegistry.initializeDefaultRoutes();
    }

    @Test
    @DisplayName("Should initialize default routes")
    void shouldInitializeDefaultRoutes() {
        assertTrue(routeRegistry.getRouteCount() > 0);
    }

    @Test
    @DisplayName("Should find route by service and path")
    void shouldFindRouteByServiceAndPath() {
        Optional<RouteDefinition> route = routeRegistry.findRoute("products", "/list", "GET");

        assertTrue(route.isPresent());
        assertEquals("products", route.get().getService());
    }

    @Test
    @DisplayName("Should find route with wildcard path pattern")
    void shouldFindRouteWithWildcardPathPattern() {
        Optional<RouteDefinition> route = routeRegistry.findRoute("orders", "/123/details", "GET");

        assertTrue(route.isPresent());
        assertEquals("orders", route.get().getService());
    }

    @Test
    @DisplayName("Should return empty when route not found")
    void shouldReturnEmptyWhenRouteNotFound() {
        Optional<RouteDefinition> route = routeRegistry.findRoute("nonexistent", "/path", "GET");

        assertTrue(route.isEmpty());
    }

    @Test
    @DisplayName("Should register new route")
    void shouldRegisterNewRoute() {
        RouteDefinition customRoute = RouteDefinition.builder()
            .id("custom-route")
            .service("custom")
            .pathPattern("/**")
            .methods(Set.of("GET", "POST"))
            .backendUrl("http://custom-service:8099")
            .enabled(true)
            .build();

        routeRegistry.registerRoute(customRoute);

        Optional<RouteDefinition> found = routeRegistry.getRoute("custom-route");
        assertTrue(found.isPresent());
        assertEquals("custom", found.get().getService());
    }

    @Test
    @DisplayName("Should unregister route")
    void shouldUnregisterRoute() {
        // Register a route first
        RouteDefinition route = RouteDefinition.builder()
            .id("temp-route")
            .service("temp")
            .pathPattern("/**")
            .methods(Set.of("GET"))
            .backendUrl("http://temp:8000")
            .enabled(true)
            .build();
        routeRegistry.registerRoute(route);

        // Verify it exists
        assertTrue(routeRegistry.getRoute("temp-route").isPresent());

        // Unregister
        routeRegistry.unregisterRoute("temp-route");

        // Verify it's gone
        assertTrue(routeRegistry.getRoute("temp-route").isEmpty());
    }

    @Test
    @DisplayName("Should enable route")
    void shouldEnableRoute() {
        // Create and register a disabled route
        RouteDefinition disabledRoute = RouteDefinition.builder()
            .id("disabled-route")
            .service("disabled")
            .pathPattern("/**")
            .methods(Set.of("GET"))
            .backendUrl("http://disabled:8000")
            .enabled(false)
            .build();
        routeRegistry.registerRoute(disabledRoute);

        assertFalse(routeRegistry.getRoute("disabled-route").get().isEnabled());

        // Enable the route
        routeRegistry.enableRoute("disabled-route");

        assertTrue(routeRegistry.getRoute("disabled-route").get().isEnabled());
    }

    @Test
    @DisplayName("Should disable route")
    void shouldDisableRoute() {
        // Get an existing enabled route
        Optional<RouteDefinition> route = routeRegistry.getRoute("products-all");
        assertTrue(route.isPresent());
        assertTrue(route.get().isEnabled());

        // Disable it
        routeRegistry.disableRoute("products-all");

        assertFalse(routeRegistry.getRoute("products-all").get().isEnabled());
    }

    @Test
    @DisplayName("Should get routes by service")
    void shouldGetRoutesByService() {
        var routes = routeRegistry.getRoutesByService("products");

        assertFalse(routes.isEmpty());
        routes.forEach(r -> assertEquals("products", r.getService()));
    }

    @Test
    @DisplayName("Should get all routes")
    void shouldGetAllRoutes() {
        var allRoutes = routeRegistry.getAllRoutes();

        assertFalse(allRoutes.isEmpty());
        assertEquals(routeRegistry.getRouteCount(), allRoutes.size());
    }
}
