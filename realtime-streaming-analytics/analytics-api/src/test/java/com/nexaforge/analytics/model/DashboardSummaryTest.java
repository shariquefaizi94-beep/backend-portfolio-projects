package com.nexaforge.analytics.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DashboardSummary record.
 */
class DashboardSummaryTest {

    @Test
    void recordCreation_withAllFields() {
        Instant computed = Instant.now();
        Map<String, Long> byType = Map.of("click", 100L, "view", 200L);
        Map<String, Long> byCountry = Map.of("US", 150L, "UK", 150L);
        Map<String, Long> byDevice = Map.of("desktop", 200L, "mobile", 100L);

        DashboardSummary summary = new DashboardSummary(
                5000L,
                500L,
                750L,
                25.5,
                byType,
                byCountry,
                byDevice,
                10,
                computed
        );

        assertEquals(5000L, summary.totalEvents());
        assertEquals(500L, summary.uniqueUsers());
        assertEquals(750L, summary.uniqueSessions());
        assertEquals(25.5, summary.avgLatencyMs());
        assertEquals(byType, summary.eventsByType());
        assertEquals(byCountry, summary.eventsByCountry());
        assertEquals(byDevice, summary.eventsByDevice());
        assertEquals(10, summary.windowsAggregated());
        assertEquals(computed, summary.computedAt());
    }

    @Test
    void recordEquality() {
        Instant computed = Instant.parse("2024-01-01T12:00:00Z");
        Map<String, Long> byType = Map.of("click", 100L);
        Map<String, Long> byCountry = Map.of("US", 100L);
        Map<String, Long> byDevice = Map.of("desktop", 100L);

        DashboardSummary s1 = new DashboardSummary(
                1000L, 100L, 150L, 20.0, byType, byCountry, byDevice, 5, computed
        );
        DashboardSummary s2 = new DashboardSummary(
                1000L, 100L, 150L, 20.0, byType, byCountry, byDevice, 5, computed
        );

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    void emptyMaps_areSupported() {
        DashboardSummary summary = new DashboardSummary(
                0L, 0L, 0L, 0.0,
                Map.of(), Map.of(), Map.of(),
                0, Instant.now()
        );

        assertTrue(summary.eventsByType().isEmpty());
        assertTrue(summary.eventsByCountry().isEmpty());
        assertTrue(summary.eventsByDevice().isEmpty());
    }

    @Test
    void zeroWindowsAggregated_isValid() {
        DashboardSummary summary = new DashboardSummary(
                0L, 0L, 0L, 0.0,
                Map.of(), Map.of(), Map.of(),
                0, Instant.now()
        );

        assertEquals(0, summary.windowsAggregated());
    }
}
