package com.portfolio.observability.alerting.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an alert instance.
 */
public record Alert(
    UUID id,
    String name,
    String description,
    AlertSeverity severity,
    AlertStatus status,
    Map<String, String> labels,
    Map<String, String> annotations,
    String generatorUrl,
    Instant startsAt,
    Instant endsAt,
    Instant lastUpdatedAt
) {
    
    public Alert {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Alert name cannot be null or blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Alert severity cannot be null");
        }
    }
    
    public static Alert create(String name, String description, AlertSeverity severity, 
                                Map<String, String> labels, Map<String, String> annotations) {
        Instant now = Instant.now();
        return new Alert(
            UUID.randomUUID(),
            name,
            description,
            severity,
            AlertStatus.PENDING,
            labels != null ? labels : Map.of(),
            annotations != null ? annotations : Map.of(),
            null,
            now,
            null,
            now
        );
    }
    
    public Alert fire() {
        return new Alert(id, name, description, severity, AlertStatus.FIRING, 
            labels, annotations, generatorUrl, startsAt, null, Instant.now());
    }
    
    public Alert resolve() {
        return new Alert(id, name, description, severity, AlertStatus.RESOLVED, 
            labels, annotations, generatorUrl, startsAt, Instant.now(), Instant.now());
    }
    
    public Alert acknowledge() {
        return new Alert(id, name, description, severity, AlertStatus.ACKNOWLEDGED, 
            labels, annotations, generatorUrl, startsAt, endsAt, Instant.now());
    }
    
    public Alert silence() {
        return new Alert(id, name, description, severity, AlertStatus.SILENCED, 
            labels, annotations, generatorUrl, startsAt, endsAt, Instant.now());
    }
    
    public boolean isFiring() {
        return status == AlertStatus.FIRING;
    }
    
    public boolean isResolved() {
        return status == AlertStatus.RESOLVED;
    }
    
    public Duration duration() {
        Instant end = endsAt != null ? endsAt : Instant.now();
        return Duration.between(startsAt, end);
    }
}
