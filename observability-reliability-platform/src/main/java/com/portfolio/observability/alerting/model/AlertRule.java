package com.portfolio.observability.alerting.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Defines an alerting rule based on metrics or conditions.
 */
public record AlertRule(
    UUID id,
    String name,
    String description,
    String expression,          // PromQL or similar query expression
    Duration forDuration,       // How long condition must be true before firing
    AlertSeverity severity,
    Map<String, String> labels,
    Map<String, String> annotations,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt
) {
    
    public AlertRule {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Alert rule name cannot be null or blank");
        }
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Alert rule expression cannot be null or blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Alert severity cannot be null");
        }
    }
    
    public static AlertRule create(String name, String description, String expression, 
                                    Duration forDuration, AlertSeverity severity) {
        Instant now = Instant.now();
        return new AlertRule(
            UUID.randomUUID(),
            name,
            description,
            expression,
            forDuration != null ? forDuration : Duration.ZERO,
            severity,
            Map.of(),
            Map.of(),
            true,
            now,
            now
        );
    }
    
    public AlertRule withLabels(Map<String, String> newLabels) {
        return new AlertRule(id, name, description, expression, forDuration, severity, 
            newLabels, annotations, enabled, createdAt, Instant.now());
    }
    
    public AlertRule withAnnotations(Map<String, String> newAnnotations) {
        return new AlertRule(id, name, description, expression, forDuration, severity, 
            labels, newAnnotations, enabled, createdAt, Instant.now());
    }
    
    public AlertRule enable() {
        return new AlertRule(id, name, description, expression, forDuration, severity, 
            labels, annotations, true, createdAt, Instant.now());
    }
    
    public AlertRule disable() {
        return new AlertRule(id, name, description, expression, forDuration, severity, 
            labels, annotations, false, createdAt, Instant.now());
    }
}
