package com.portfolio.observability.health.repository;

import com.portfolio.observability.health.model.ComponentHealth;
import com.portfolio.observability.health.model.HealthStatus;
import com.portfolio.observability.health.model.ServiceHealth;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for service health tracking.
 * Maintains current health state and historical health data for services and their components.
 */
@Repository
public class HealthRepository {

    private final Map<String, ServiceHealth> serviceHealthMap = new ConcurrentHashMap<>();
    private final Map<String, List<ServiceHealth>> healthHistory = new ConcurrentHashMap<>();

    public ServiceHealth save(ServiceHealth serviceHealth) {
        serviceHealthMap.put(serviceHealth.serviceName(), serviceHealth);
        
        // Keep history
        healthHistory.computeIfAbsent(serviceHealth.serviceName(), k -> 
            Collections.synchronizedList(new ArrayList<>())
        ).add(serviceHealth);
        
        return serviceHealth;
    }

    public Optional<ServiceHealth> findByServiceName(String serviceName) {
        return Optional.ofNullable(serviceHealthMap.get(serviceName));
    }

    public List<ServiceHealth> findAll() {
        return new ArrayList<>(serviceHealthMap.values());
    }

    public List<ServiceHealth> findByStatus(HealthStatus status) {
        return serviceHealthMap.values().stream()
                .filter(h -> h.status() == status)
                .sorted(Comparator.comparing(ServiceHealth::checkedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<ServiceHealth> findUnhealthy() {
        return serviceHealthMap.values().stream()
                .filter(h -> h.status() != HealthStatus.UP)
                .sorted(Comparator.comparing(h -> h.status().ordinal()))
                .collect(Collectors.toList());
    }

    public List<ServiceHealth> findDegraded() {
        return serviceHealthMap.values().stream()
                .filter(h -> h.status() == HealthStatus.DEGRADED || h.status() == HealthStatus.DOWN)
                .collect(Collectors.toList());
    }

    public List<ServiceHealth> findHistory(String serviceName, Instant start, Instant end) {
        return healthHistory.getOrDefault(serviceName, Collections.emptyList()).stream()
                .filter(h -> !h.checkedAt().isBefore(start) && !h.checkedAt().isAfter(end))
                .sorted(Comparator.comparing(ServiceHealth::checkedAt))
                .collect(Collectors.toList());
    }

    public Map<HealthStatus, Long> getStatusDistribution() {
        return serviceHealthMap.values().stream()
                .collect(Collectors.groupingBy(ServiceHealth::status, Collectors.counting()));
    }

    public double calculateOverallHealthScore() {
        if (serviceHealthMap.isEmpty()) {
            return 100.0;
        }
        
        long totalWeight = 0;
        long weightedScore = 0;
        
        for (ServiceHealth health : serviceHealthMap.values()) {
            int weight = 1; // All services have equal weight
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

    public List<ComponentHealth> findUnhealthyComponents() {
        return serviceHealthMap.values().stream()
                .flatMap(h -> h.components().stream())
                .filter(c -> c.status() != HealthStatus.UP)
                .collect(Collectors.toList());
    }

    public Map<String, HealthStatus> getServiceStatusMap() {
        return serviceHealthMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().status()
                ));
    }

    public long countByStatus(HealthStatus status) {
        return serviceHealthMap.values().stream()
                .filter(h -> h.status() == status)
                .count();
    }

    public double calculateUptimePercentage(String serviceName, Instant start, Instant end) {
        List<ServiceHealth> history = findHistory(serviceName, start, end);
        if (history.isEmpty()) {
            return 100.0;
        }
        
        long upCount = history.stream()
                .filter(h -> h.status() == HealthStatus.UP)
                .count();
        
        return (double) upCount / history.size() * 100.0;
    }

    public void deleteByServiceName(String serviceName) {
        serviceHealthMap.remove(serviceName);
        healthHistory.remove(serviceName);
    }

    public void deleteHistoryOlderThan(Instant threshold) {
        healthHistory.values().forEach(list -> 
            list.removeIf(h -> h.checkedAt().isBefore(threshold))
        );
    }

    public List<String> findAllServiceNames() {
        return new ArrayList<>(serviceHealthMap.keySet());
    }

    public boolean exists(String serviceName) {
        return serviceHealthMap.containsKey(serviceName);
    }

    public void clear() {
        serviceHealthMap.clear();
        healthHistory.clear();
    }
}
