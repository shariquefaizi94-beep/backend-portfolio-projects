package com.portfolio.observability.health.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregated health status for a service.
 */
public record ServiceHealth(
    UUID id,
    String serviceName,
    String serviceVersion,
    HealthStatus status,
    List<ComponentHealth> components,
    Map<String, Object> details,
    Instant checkedAt
) {
    
    public ServiceHealth {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("Service name cannot be null or blank");
        }
    }
    
    public static ServiceHealth healthy(String serviceName, String version) {
        return new ServiceHealth(
            UUID.randomUUID(),
            serviceName,
            version,
            HealthStatus.UP,
            List.of(),
            Map.of(),
            Instant.now()
        );
    }
    
    public static ServiceHealth unhealthy(String serviceName, String version, String reason) {
        return new ServiceHealth(
            UUID.randomUUID(),
            serviceName,
            version,
            HealthStatus.DOWN,
            List.of(),
            Map.of("reason", reason),
            Instant.now()
        );
    }
    
    public static ServiceHealth aggregate(String serviceName, String version, List<ComponentHealth> components) {
        HealthStatus overallStatus = calculateOverallStatus(components);
        return new ServiceHealth(
            UUID.randomUUID(),
            serviceName,
            version,
            overallStatus,
            components,
            Map.of(),
            Instant.now()
        );
    }
    
    private static HealthStatus calculateOverallStatus(List<ComponentHealth> components) {
        if (components == null || components.isEmpty()) {
            return HealthStatus.UP;
        }
        
        boolean hasDown = components.stream().anyMatch(c -> c.status() == HealthStatus.DOWN);
        boolean hasDegraded = components.stream().anyMatch(c -> c.status() == HealthStatus.DEGRADED);
        
        if (hasDown) return HealthStatus.DOWN;
        if (hasDegraded) return HealthStatus.DEGRADED;
        return HealthStatus.UP;
    }
    
    public boolean isHealthy() {
        return status == HealthStatus.UP;
    }
    
    public boolean isDegraded() {
        return status == HealthStatus.DEGRADED;
    }
}
