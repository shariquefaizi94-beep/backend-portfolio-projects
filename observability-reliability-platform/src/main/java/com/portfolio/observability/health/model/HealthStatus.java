package com.portfolio.observability.health.model;

/**
 * Health status of a service or component.
 */
public enum HealthStatus {
    UP,         // Service is healthy
    DOWN,       // Service is unhealthy
    DEGRADED,   // Service is partially available
    UNKNOWN     // Health cannot be determined
}
