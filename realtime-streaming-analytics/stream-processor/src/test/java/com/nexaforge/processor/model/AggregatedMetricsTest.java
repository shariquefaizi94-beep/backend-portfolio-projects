package com.nexaforge.processor.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AggregatedMetrics record.
 */
class AggregatedMetricsTest {

    @Test
    void recordCreation_withAllFields() {
        Instant start = Instant.now().minusSeconds(60);
        Instant end = Instant.now();
        Instant computed = Instant.now();

        Map<String, Long> byType = Map.of("click", 100L, "view", 200L);
        Map<String, Long> byCountry = Map.of("US", 150L, "UK", 150L);
        Map<String, Long> byDevice = Map.of("desktop", 200L, "mobile", 100L);
        Map<String, Long> topPages = Map.of("/home", 180L, "/products", 120L);

        AggregatedMetrics metrics = new AggregatedMetrics(
                "window-2024-01",
                start,
                end,
                300L,
                50L,
                75L,
                byType,
                byCountry,
                byDevice,
                topPages,
                15.5,
                100L,
                computed
        );

        assertEquals("window-2024-01", metrics.windowId());
        assertEquals(start, metrics.windowStart());
        assertEquals(end, metrics.windowEnd());
        assertEquals(300L, metrics.totalEvents());
        assertEquals(50L, metrics.uniqueUsers());
        assertEquals(75L, metrics.uniqueSessions());
        assertEquals(byType, metrics.eventsByType());
        assertEquals(byCountry, metrics.eventsByCountry());
        assertEquals(byDevice, metrics.eventsByDevice());
        assertEquals(topPages, metrics.topPages());
        assertEquals(15.5, metrics.avgLatencyMs());
        assertEquals(100L, metrics.maxLatencyMs());
        assertEquals(computed, metrics.computedAt());
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

    @Test
    void emptyMaps_areSupported() {
        AggregatedMetrics metrics = new AggregatedMetrics(
                "w1", Instant.now(), Instant.now(), 0, 0, 0,
                Map.of(), Map.of(), Map.of(), Map.of(),
                0.0, 0, Instant.now()
        );

        assertTrue(metrics.eventsByType().isEmpty());
        assertTrue(metrics.eventsByCountry().isEmpty());
        assertTrue(metrics.eventsByDevice().isEmpty());
        assertTrue(metrics.topPages().isEmpty());
    }
}
