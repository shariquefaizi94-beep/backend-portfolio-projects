package com.nexaforge.processor.model;

import java.time.Instant;
import java.util.Map;

/**
 * Windowed aggregation result — stored in Elasticsearch for querying.
 * Represents metrics computed over a time window (e.g., 1 minute).
 */
public record AggregatedMetrics(
        String windowId,
        Instant windowStart,
        Instant windowEnd,
        long totalEvents,
        long uniqueUsers,
        long uniqueSessions,
        Map<String, Long> eventsByType,
        Map<String, Long> eventsByCountry,
        Map<String, Long> eventsByDevice,
        Map<String, Long> topPages,
        double avgLatencyMs,
        long maxLatencyMs,
        Instant computedAt
) {}
