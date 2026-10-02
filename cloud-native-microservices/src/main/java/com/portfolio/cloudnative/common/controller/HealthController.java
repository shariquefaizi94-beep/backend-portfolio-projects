package com.portfolio.cloudnative.common.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Health controller for Kubernetes probes and service mesh health checks.
 * Provides detailed health information for monitoring and observability.
 */
@RestController
@RequestMapping("/health")
public class HealthController {
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Instant startTime;
    
    public HealthController(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.startTime = Instant.now();
    }
    
    /**
     * Liveness probe - used by Kubernetes to determine if the pod should be restarted.
     * Should return quickly and only check if the application is running.
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Readiness probe - used by Kubernetes to determine if the pod should receive traffic.
     * Checks if the application is ready to serve requests.
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> response = new HashMap<>();
        
        // Check circuit breakers
        boolean allCircuitBreakersClosed = circuitBreakerRegistry.getAllCircuitBreakers().stream()
            .allMatch(cb -> cb.getState() != CircuitBreaker.State.OPEN);
        
        if (allCircuitBreakersClosed) {
            response.put("status", "UP");
            response.put("ready", true);
        } else {
            response.put("status", "DEGRADED");
            response.put("ready", true); // Still accept traffic but note degradation
        }
        
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Detailed health check for monitoring dashboards.
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        response.put("uptime", java.time.Duration.between(startTime, Instant.now()).toString());
        
        // Circuit breaker states
        Map<String, String> circuitBreakers = circuitBreakerRegistry.getAllCircuitBreakers().stream()
            .collect(Collectors.toMap(
                CircuitBreaker::getName,
                cb -> cb.getState().toString()
            ));
        response.put("circuitBreakers", circuitBreakers);
        
        // System info
        Map<String, Object> system = new HashMap<>();
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        system.put("maxMemory", Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB");
        system.put("freeMemory", Runtime.getRuntime().freeMemory() / (1024 * 1024) + " MB");
        response.put("system", system);
        
        return ResponseEntity.ok(response);
    }
}
