package com.portfolio.observability.metrics.model;

/**
 * Types of metrics that can be collected.
 */
public enum MetricType {
    COUNTER,    // Monotonically increasing value
    GAUGE,      // Value that can go up or down
    HISTOGRAM,  // Distribution of values
    SUMMARY,    // Similar to histogram with percentiles
    TIMER       // Specialized histogram for duration
}
