package com.portfolio.observability.slo.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service Level Objective definition.
 */
public record ServiceLevelObjective(
    UUID id,
    String name,
    String description,
    String service,
    SloType type,
    double target,              // Target percentage (e.g., 99.9)
    Duration window,            // Rolling window (e.g., 30 days)
    String sliExpression,       // SLI calculation expression
    Map<String, String> labels,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt
) {
    
    public ServiceLevelObjective {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("SLO name cannot be null or blank");
        }
        if (service == null || service.isBlank()) {
            throw new IllegalArgumentException("Service cannot be null or blank");
        }
        if (target <= 0 || target > 100) {
            throw new IllegalArgumentException("Target must be between 0 and 100");
        }
        if (window == null || window.isNegative()) {
            throw new IllegalArgumentException("Window must be a positive duration");
        }
    }
    
    public static ServiceLevelObjective availability(String name, String service, 
                                                      double target, Duration window) {
        Instant now = Instant.now();
        return new ServiceLevelObjective(
            UUID.randomUUID(),
            name,
            "Availability SLO for " + service,
            service,
            SloType.AVAILABILITY,
            target,
            window,
            String.format("sum(rate(http_requests_total{service=\"%s\",status!~\"5..\"}[5m])) / " +
                         "sum(rate(http_requests_total{service=\"%s\"}[5m])) * 100", service, service),
            Map.of(),
            true,
            now,
            now
        );
    }
    
    public static ServiceLevelObjective latency(String name, String service, 
                                                 double target, Duration window, Duration threshold) {
        Instant now = Instant.now();
        return new ServiceLevelObjective(
            UUID.randomUUID(),
            name,
            "Latency SLO for " + service + " - " + target + "% of requests under " + threshold.toMillis() + "ms",
            service,
            SloType.LATENCY,
            target,
            window,
            String.format("histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket{service=\"%s\"}[5m])) by (le))", service),
            Map.of("threshold_ms", String.valueOf(threshold.toMillis())),
            true,
            now,
            now
        );
    }
    
    public double errorBudgetPercentage() {
        return 100.0 - target;
    }
    
    public Duration errorBudgetDuration() {
        double fraction = (100.0 - target) / 100.0;
        return Duration.ofSeconds((long) (window.getSeconds() * fraction));
    }
}
