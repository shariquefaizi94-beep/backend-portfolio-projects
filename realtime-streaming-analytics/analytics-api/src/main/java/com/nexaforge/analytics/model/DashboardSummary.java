package com.nexaforge.analytics.model;

import java.time.Instant;
import java.util.Map;

/**
 * Aggregated summary across multiple windows for dashboard display.
 */
public record DashboardSummary(
        long totalEvents,
        long uniqueUsers,
        long uniqueSessions,
        double avgLatencyMs,
        Map<String, Long> eventsByType,
        Map<String, Long> eventsByCountry,
        Map<String, Long> eventsByDevice,
        int windowsAggregated,
        Instant computedAt
) {}
