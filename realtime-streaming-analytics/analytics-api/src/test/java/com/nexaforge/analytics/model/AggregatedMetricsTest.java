package com.nexaforge.analytics.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AggregatedMetrics record in analytics-api module.
 */
class AggregatedMetricsTest {

    @Test
    void recordCreation_withAllFields() {
        Instant start = Instant.now().minusSeconds(60);
        Instant end = Instant.now();
        Instant computed = Instant.now();

        AggregatedMetrics metrics = new AggregatedMetrics(
                "window-123",
                start,
                end,
                1500L,
                200L,
                300L,
                Map.of("click", 1000L, "view", 500L),
                Map.of("US", 1000L, "UK", 500L),
                Map.of("desktop", 900L, "mobile", 600L),
                Map.of("/home", 800L),
                18.5,
                75L,
                computed
        );

        assertEquals("window-123", metrics.windowId());
        assertEquals(start, metrics.windowStart());
        assertEquals(end, metrics.windowEnd());
        assertEquals(1500L, metrics.totalEvents());
        assertEquals(200L, metrics.uniqueUsers());
        assertEquals(300L, metrics.uniqueSessions());
        assertEquals(18.5, metrics.avgLatencyMs());
        assertEquals(75L, metrics.maxLatencyMs());
    }

    @Test
    void recordEquality() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T00:01:00Z");
        Instant computed = Instant.parse("2024-01-01T00:01:01Z");

        AggregatedMetrics m1 = new AggregatedMetrics(
                "w1", start, end, 100, 10, 20,
                Map.of(), Map.of(), Map.of(), Map.of(),
                10.0, 50, computed
        );
        AggregatedMetrics m2 = new AggregatedMetrics(
                "w1", start, end, 100, 10, 20,
                Map.of(), Map.of(), Map.of(), Map.of(),
                10.0, 50, computed
        );

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }
}
