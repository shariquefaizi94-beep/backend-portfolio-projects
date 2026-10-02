package com.portfolio.observability.alerting.model;

/**
 * Status of an alert.
 */
public enum AlertStatus {
    PENDING,     // Alert condition detected but not yet firing
    FIRING,      // Alert is actively firing
    RESOLVED,    // Alert condition no longer met
    SILENCED,    // Alert is silenced/muted
    ACKNOWLEDGED // Alert acknowledged by operator
}
