package com.portfolio.observability.alerting.model;

/**
 * Severity levels for alerts.
 */
public enum AlertSeverity {
    INFO,       // Informational alert
    WARNING,    // Warning that may need attention
    CRITICAL,   // Critical issue requiring immediate action
    PAGE        // Requires immediate paging/on-call
}
