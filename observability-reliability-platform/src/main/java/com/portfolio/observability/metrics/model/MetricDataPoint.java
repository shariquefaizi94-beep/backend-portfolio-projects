package com.portfolio.observability.metrics.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single metric data point.
 */
public record MetricDataPoint(
    UUID id,
    String metricName,
    double value,
    Map<String, String> labels,
    Instant timestamp
) {
    
    public MetricDataPoint {
        if (metricName == null || metricName.isBlank()) {
            throw new IllegalArgumentException("Metric name cannot be null or blank");
        }
        if (labels == null) {
            labels = Map.of();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
    
    public static MetricDataPoint of(String metricName, double value) {
        return new MetricDataPoint(UUID.randomUUID(), metricName, value, Map.of(), Instant.now());
    }
    
    public static MetricDataPoint of(String metricName, double value, Map<String, String> labels) {
        return new MetricDataPoint(UUID.randomUUID(), metricName, value, labels, Instant.now());
    }
    
    public static MetricDataPoint of(String metricName, double value, Map<String, String> labels, Instant timestamp) {
        return new MetricDataPoint(UUID.randomUUID(), metricName, value, labels, timestamp);
    }
}
