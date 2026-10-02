package com.nexaforge.analytics.model;

import java.time.Instant;
import java.util.Map;

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
