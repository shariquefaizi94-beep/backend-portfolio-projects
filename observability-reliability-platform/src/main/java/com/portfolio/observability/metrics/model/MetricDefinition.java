package com.portfolio.observability.metrics.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Defines a metric to be collected and monitored.
 */
public record MetricDefinition(
    UUID id,
    String name,
    String description,
    MetricType type,
    String unit,
    List<String> labels,
    Map<String, String> tags,
    boolean enabled,
    Instant createdAt
) {
    
    public MetricDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Metric name cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Metric type cannot be null");
        }
    }
    
    public static MetricDefinition counter(String name, String description, List<String> labels) {
        return new MetricDefinition(
            UUID.randomUUID(),
            name,
            description,
            MetricType.COUNTER,
            "count",
            labels != null ? labels : List.of(),
            Map.of(),
            true,
            Instant.now()
        );
    }
    
    public static MetricDefinition gauge(String name, String description, String unit) {
        return new MetricDefinition(
            UUID.randomUUID(),
            name,
            description,
            MetricType.GAUGE,
            unit,
            List.of(),
            Map.of(),
            true,
            Instant.now()
        );
    }
    
    public static MetricDefinition timer(String name, String description, List<String> labels) {
        return new MetricDefinition(
            UUID.randomUUID(),
            name,
            description,
            MetricType.TIMER,
            "seconds",
            labels != null ? labels : List.of(),
            Map.of(),
            true,
            Instant.now()
        );
    }
    
    public static MetricDefinition histogram(String name, String description, String unit, List<String> labels) {
        return new MetricDefinition(
            UUID.randomUUID(),
            name,
            description,
            MetricType.HISTOGRAM,
            unit,
            labels != null ? labels : List.of(),
            Map.of(),
            true,
            Instant.now()
        );
    }
}
