package com.portfolio.api.gateway;

import com.portfolio.api.gateway.model.ApiRequest;
import com.portfolio.api.gateway.model.ApiResponse;
import com.portfolio.api.gateway.routing.RouteDefinition;
import com.portfolio.api.gateway.routing.RouteRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * API Gateway controller that handles routing, transformation, and forwarding.
 * Demonstrates gateway pattern with dynamic routing and request/response transformation.
 */
@RestController
@RequestMapping("/gateway")
public class ApiGatewayController {
    
    private static final Logger log = LoggerFactory.getLogger(ApiGatewayController.class);
    
    private final RouteRegistry routeRegistry;
    private final RequestTransformer requestTransformer;
    private final ResponseTransformer responseTransformer;
    private final Timer gatewayLatency;
    
    public ApiGatewayController(RouteRegistry routeRegistry,
                                 RequestTransformer requestTransformer,
                                 ResponseTransformer responseTransformer,
                                 MeterRegistry meterRegistry) {
        this.routeRegistry = routeRegistry;
        this.requestTransformer = requestTransformer;
        this.responseTransformer = responseTransformer;
        this.gatewayLatency = meterRegistry.timer("gateway.request.latency");
    }
    
    /**
     * Main gateway endpoint - routes requests to appropriate backend services.
     */
    @RequestMapping(value = "/api/{service}/**", method = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, 
        RequestMethod.DELETE, RequestMethod.PATCH
    })
    public ResponseEntity<ApiResponse> routeRequest(
            @PathVariable String service,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        
        return gatewayLatency.record(() -> {
            String path = extractPath(request, service);
            String method = request.getMethod();
            
            log.info("Gateway routing: {} {} -> service: {}", method, path, service);
            
            // Find matching route
            Optional<RouteDefinition> route = routeRegistry.findRoute(service, path, method);
            
            if (route.isEmpty()) {
                log.warn("No route found for service: {}, path: {}", service, path);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Route not found", "ROUTE_NOT_FOUND"));
            }
            
            RouteDefinition routeDef = route.get();
            
            // Check if route is enabled
            if (!routeDef.isEnabled()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Service temporarily unavailable", "SERVICE_DISABLED"));
            }
            
            // Build API request
            ApiRequest apiRequest = ApiRequest.builder()
                .service(service)
                .path(path)
                .method(method)
                .headers(extractHeaders(request))
                .queryParams(extractQueryParams(request))
                .body(body)
                .clientIp(getClientIp(request))
                .timestamp(Instant.now())
                .build();
            
            // Transform request
            ApiRequest transformedRequest = requestTransformer.transform(apiRequest, routeDef);
            
            // In a real implementation, this would forward to the actual backend service
            // For demo purposes, we simulate a successful response
            ApiResponse response = simulateBackendCall(transformedRequest, routeDef);
            
            // Transform response
            ApiResponse transformedResponse = responseTransformer.transform(response, routeDef);
            
            return ResponseEntity.ok(transformedResponse);
        });
    }
    
    /**
     * Health check endpoint for the gateway.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "api-gateway");
        health.put("timestamp", Instant.now().toString());
        health.put("routes", routeRegistry.getRouteCount());
        return ResponseEntity.ok(health);
    }
    
    /**
     * Returns all registered routes.
     */
    @GetMapping("/routes")
    public ResponseEntity<Map<String, Object>> listRoutes() {
        Map<String, Object> response = new HashMap<>();
        response.put("routes", routeRegistry.getAllRoutes());
        response.put("count", routeRegistry.getRouteCount());
        return ResponseEntity.ok(response);
    }
    
    private String extractPath(HttpServletRequest request, String service) {
        String fullPath = request.getRequestURI();
        String prefix = "/gateway/api/" + service;
        return fullPath.length() > prefix.length() 
            ? fullPath.substring(prefix.length()) 
            : "/";
    }
    
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            // Skip sensitive headers
            if (!name.equalsIgnoreCase("authorization") && 
                !name.equalsIgnoreCase("cookie")) {
                headers.put(name, request.getHeader(name));
            }
        }
        return headers;
    }
    
    private Map<String, String> extractQueryParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    private ApiResponse simulateBackendCall(ApiRequest request, RouteDefinition route) {
        // Simulates backend service response for demo purposes
        Map<String, Object> data = new HashMap<>();
        data.put("service", request.service());
        data.put("path", request.path());
        data.put("method", request.method());
        data.put("routeId", route.getId());
        data.put("backend", route.getBackendUrl());
        data.put("processed", true);
        
        return ApiResponse.success(data);
    }
}
