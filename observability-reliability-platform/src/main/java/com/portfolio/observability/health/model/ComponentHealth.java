package com.portfolio.observability.health.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Health status of an individual component.
 */
public record ComponentHealth(
    String name,
    HealthStatus status,
    Duration responseTime,
    Map<String, Object> details,
    Instant checkedAt
) {
    
    public ComponentHealth {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Component name cannot be null or blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }
    
    public static ComponentHealth up(String name, Duration responseTime) {
        return new ComponentHealth(name, HealthStatus.UP, responseTime, Map.of(), Instant.now());
    }
    
    public static ComponentHealth down(String name, String reason) {
        return new ComponentHealth(name, HealthStatus.DOWN, null, Map.of("reason", reason), Instant.now());
    }
    
    public static ComponentHealth unhealthy(String name, String reason) {
        return down(name, reason);
    }
    
    public static ComponentHealth degraded(String name, Duration responseTime, String reason) {
        return new ComponentHealth(name, HealthStatus.DEGRADED, responseTime, 
            Map.of("reason", reason), Instant.now());
    }
    
    public boolean isHealthy() {
        return status == HealthStatus.UP;
    }
}
