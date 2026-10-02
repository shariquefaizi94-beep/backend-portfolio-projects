package com.portfolio.observability.health.service;

import com.portfolio.observability.health.model.ComponentHealth;
import com.portfolio.observability.health.model.HealthStatus;
import com.portfolio.observability.health.model.ServiceHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Service for managing health checks and service health monitoring.
 * Provides comprehensive health monitoring capabilities including dependency tracking.
 */
@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private final Map<String, ServiceHealth> currentHealth = new ConcurrentHashMap<>();
    private final Map<String, List<ServiceHealth>> healthHistory = new ConcurrentHashMap<>();
    private final Map<String, HealthCheckDefinition> healthChecks = new ConcurrentHashMap<>();

    // Health Check Registration

    public void registerHealthCheck(String name, Supplier<ComponentHealth> healthCheck) {
        registerHealthCheck(name, healthCheck, Duration.ofSeconds(30), false);
    }

    public void registerHealthCheck(String name, Supplier<ComponentHealth> healthCheck, 
                                     Duration interval, boolean critical) {
        healthChecks.put(name, new HealthCheckDefinition(name, healthCheck, interval, critical));
        log.info("Registered health check: {} (critical: {}, interval: {})", name, critical, interval);
    }

    public void unregisterHealthCheck(String name) {
        healthChecks.remove(name);
        log.info("Unregistered health check: {}", name);
    }

    public List<String> getRegisteredHealthChecks() {
        return new ArrayList<>(healthChecks.keySet());
    }

    // Service Health Management

    public ServiceHealth recordHealth(ServiceHealth health) {
        currentHealth.put(health.serviceName(), health);
        
        // Store in history
        healthHistory.computeIfAbsent(health.serviceName(), k -> 
            Collections.synchronizedList(new ArrayList<>())
        ).add(health);
        
        log.debug("Recorded health for service: {} status: {}", health.serviceName(), health.status());
        return health;
    }

    public ServiceHealth checkService(String serviceName, String version) {
        List<ComponentHealth> componentHealths = executeHealthChecks();
        ServiceHealth health = ServiceHealth.aggregate(serviceName, version, componentHealths);
        return recordHealth(health);
    }

    private List<ComponentHealth> executeHealthChecks() {
        return healthChecks.values().stream()
                .map(this::executeHealthCheck)
                .collect(Collectors.toList());
    }

    private ComponentHealth executeHealthCheck(HealthCheckDefinition definition) {
        try {
            return definition.healthCheck().get();
        } catch (Exception e) {
            log.error("Health check failed: {} - {}", definition.name(), e.getMessage());
            return ComponentHealth.unhealthy(definition.name(), e.getMessage());
        }
    }

    // Health Queries

    public Optional<ServiceHealth> getServiceHealth(String serviceName) {
        return Optional.ofNullable(currentHealth.get(serviceName));
    }

    public List<ServiceHealth> getAllServiceHealth() {
        return new ArrayList<>(currentHealth.values());
    }

    public List<ServiceHealth> getServicesByStatus(HealthStatus status) {
        return currentHealth.values().stream()
                .filter(h -> h.status() == status)
                .collect(Collectors.toList());
    }

    public List<ServiceHealth> getUnhealthyServices() {
        return currentHealth.values().stream()
                .filter(h -> h.status() != HealthStatus.UP)
                .sorted(Comparator.comparing(h -> h.status().ordinal()))
                .collect(Collectors.toList());
    }

    public List<ServiceHealth> getDegradedServices() {
        return currentHealth.values().stream()
                .filter(h -> h.status() == HealthStatus.DEGRADED)
                .collect(Collectors.toList());
    }

    public List<ServiceHealth> getDownServices() {
        return currentHealth.values().stream()
                .filter(h -> h.status() == HealthStatus.DOWN)
                .collect(Collectors.toList());
    }

    public Map<String, HealthStatus> getServiceStatusMap() {
        return currentHealth.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().status()
                ));
    }

    // History Queries

    public List<ServiceHealth> getHealthHistory(String serviceName, Instant start, Instant end) {
        return healthHistory.getOrDefault(serviceName, Collections.emptyList()).stream()
                .filter(h -> !h.checkedAt().isBefore(start) && !h.checkedAt().isAfter(end))
                .sorted(Comparator.comparing(ServiceHealth::checkedAt))
                .collect(Collectors.toList());
    }

    public double calculateUptimePercentage(String serviceName, Duration window) {
        Instant start = Instant.now().minus(window);
        List<ServiceHealth> history = getHealthHistory(serviceName, start, Instant.now());
        
        if (history.isEmpty()) {
            return 100.0;
        }
        
        long upCount = history.stream()
                .filter(h -> h.status() == HealthStatus.UP)
                .count();
        
        return (double) upCount / history.size() * 100.0;
    }

    public Duration calculateDowntime(String serviceName, Duration window) {
        Instant start = Instant.now().minus(window);
        List<ServiceHealth> history = getHealthHistory(serviceName, start, Instant.now());
        
        if (history.isEmpty()) {
            return Duration.ZERO;
        }
        
        long downCount = history.stream()
                .filter(h -> h.status() == HealthStatus.DOWN)
                .count();
        
        // Estimate downtime based on check frequency
        // Assumes checks are roughly evenly distributed
        return Duration.ofMinutes(downCount); // Simplified calculation
    }

    // Component Health

    public List<ComponentHealth> getUnhealthyComponents() {
        return currentHealth.values().stream()
                .flatMap(h -> h.components().stream())
                .filter(c -> c.status() != HealthStatus.UP)
                .collect(Collectors.toList());
    }

    public Map<String, List<ComponentHealth>> getComponentHealthByService() {
        return currentHealth.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().components()
                ));
    }

    // Overall Health Score

    public OverallHealthSummary getOverallHealthSummary() {
        if (currentHealth.isEmpty()) {
            return new OverallHealthSummary(HealthStatus.UNKNOWN, 100.0, 0, 0, 0, 0, List.of());
        }
        
        Map<HealthStatus, Long> distribution = getStatusDistribution();
        long total = currentHealth.size();
        long up = distribution.getOrDefault(HealthStatus.UP, 0L);
        long degraded = distribution.getOrDefault(HealthStatus.DEGRADED, 0L);
        long down = distribution.getOrDefault(HealthStatus.DOWN, 0L);
        long unknown = distribution.getOrDefault(HealthStatus.UNKNOWN, 0L);
        
        // Calculate weighted health score
        double healthScore = calculateHealthScore();
        
        // Determine overall status
        HealthStatus overallStatus;
        if (down > 0) {
            // Check if any critical service is down
            boolean criticalDown = currentHealth.values().stream()
                    .anyMatch(h -> h.status() == HealthStatus.DOWN && isCriticalService(h.serviceName()));
            overallStatus = criticalDown ? HealthStatus.DOWN : HealthStatus.DEGRADED;
        } else if (degraded > 0) {
            overallStatus = HealthStatus.DEGRADED;
        } else if (up == total) {
            overallStatus = HealthStatus.UP;
        } else {
            overallStatus = HealthStatus.UNKNOWN;
        }
        
        List<String> issues = getUnhealthyServices().stream()
                .map(h -> h.serviceName() + ": " + h.status())
                .collect(Collectors.toList());
        
        return new OverallHealthSummary(overallStatus, healthScore, up, degraded, down, unknown, issues);
    }

    private double calculateHealthScore() {
        if (currentHealth.isEmpty()) {
            return 100.0;
        }
        
        long totalWeight = 0;
        long weightedScore = 0;
        
        for (ServiceHealth health : currentHealth.values()) {
            int weight = isCriticalService(health.serviceName()) ? 3 : 1;
            totalWeight += weight;
            
            int score = switch (health.status()) {
                case UP -> 100;
                case DEGRADED -> 50;
                case DOWN -> 0;
                case UNKNOWN -> 25;
            };
            
            weightedScore += (long) score * weight;
        }
        
        return (double) weightedScore / totalWeight;
    }

    private boolean isCriticalService(String serviceName) {
        return healthChecks.values().stream()
                .filter(h -> h.name().equals(serviceName))
                .anyMatch(HealthCheckDefinition::critical);
    }

    public Map<HealthStatus, Long> getStatusDistribution() {
        return currentHealth.values().stream()
                .collect(Collectors.groupingBy(ServiceHealth::status, Collectors.counting()));
    }

    // Statistics

    public HealthStatistics getStatistics() {
        Map<HealthStatus, Long> distribution = getStatusDistribution();
        long total = currentHealth.size();
        long up = distribution.getOrDefault(HealthStatus.UP, 0L);
        long degraded = distribution.getOrDefault(HealthStatus.DEGRADED, 0L);
        long down = distribution.getOrDefault(HealthStatus.DOWN, 0L);
        
        int unhealthyComponents = getUnhealthyComponents().size();
        double healthScore = calculateHealthScore();
        
        return new HealthStatistics(total, up, degraded, down, unhealthyComponents, 
                healthChecks.size(), healthScore);
    }

    // Maintenance

    public void pruneHealthHistory(Duration retention) {
        Instant threshold = Instant.now().minus(retention);
        healthHistory.values().forEach(list -> 
            list.removeIf(h -> h.checkedAt().isBefore(threshold))
        );
    }

    public void removeService(String serviceName) {
        currentHealth.remove(serviceName);
        healthHistory.remove(serviceName);
    }

    public void clear() {
        currentHealth.clear();
        healthHistory.clear();
        healthChecks.clear();
    }

    // Inner types

    private record HealthCheckDefinition(
            String name,
            Supplier<ComponentHealth> healthCheck,
            Duration interval,
            boolean critical
    ) {}

    public record OverallHealthSummary(
            HealthStatus status,
            double healthScore,
            long servicesUp,
            long servicesDegraded,
            long servicesDown,
            long servicesUnknown,
            List<String> issues
    ) {}

    public record HealthStatistics(
            long totalServices,
            long servicesUp,
            long servicesDegraded,
            long servicesDown,
            int unhealthyComponents,
            int registeredHealthChecks,
            double healthScore
    ) {}
}
