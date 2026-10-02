package com.portfolio.observability.slo.model;

/**
 * Types of SLO calculations.
 */
public enum SloType {
    AVAILABILITY,   // Percentage of successful requests
    LATENCY,        // Percentage of requests within latency threshold
    THROUGHPUT,     // Requests per second target
    ERROR_RATE,     // Maximum error rate threshold
    FRESHNESS       // Data freshness within time window
}
