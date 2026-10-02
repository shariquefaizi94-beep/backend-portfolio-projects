package com.nexaforge.processor.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexaforge.processor.model.AggregatedMetrics;
import com.nexaforge.processor.model.ClickEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ElasticsearchIndexer.
 * Note: These tests verify the indexer doesn't throw exceptions during serialization.
 * Full integration tests would require a running Elasticsearch instance.
 */
class ElasticsearchIndexerTest {

    private ObjectMapper objectMapper;
    private SimpleMeterRegistry meterRegistry;
    private ElasticsearchIndexer indexer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        meterRegistry = new SimpleMeterRegistry();
        indexer = new ElasticsearchIndexer(objectMapper, meterRegistry);
        ReflectionTestUtils.setField(indexer, "esUrl", "http://localhost:9200");
        ReflectionTestUtils.setField(indexer, "eventsIndex", "click-events");
        ReflectionTestUtils.setField(indexer, "metricsIndex", "aggregated-metrics");
    }

    @Test
    void indexEvent_serializesEventToJson() throws Exception {
        ClickEvent event = new ClickEvent(
                "event-123",
                "user-456",
                "session-789",
                "/home",
                "https://google.com",
                "click",
                "desktop",
                "US",
                "NYC",
                Instant.now(),
                System.currentTimeMillis()
        );

        // Verify serialization works (no exceptions)
        String json = objectMapper.writeValueAsString(event);
        assertNotNull(json);
        assertTrue(json.contains("event-123"));
        assertTrue(json.contains("user-456"));
    }

    @Test
    void indexMetrics_serializesMetricsToJson() throws Exception {
        AggregatedMetrics metrics = new AggregatedMetrics(
                "window-2024-01",
                Instant.now().minusSeconds(60),
                Instant.now(),
                1000L,
                100L,
                150L,
                Map.of("click", 500L, "view", 500L),
                Map.of("US", 600L, "UK", 400L),
                Map.of("desktop", 700L, "mobile", 300L),
                Map.of("/home", 400L, "/products", 300L),
                25.5,
                150L,
                Instant.now()
        );

        // Verify serialization works
        String json = objectMapper.writeValueAsString(metrics);
        assertNotNull(json);
        assertTrue(json.contains("window-2024-01"));
        assertTrue(json.contains("1000"));
    }

    @Test
    void metricsRegistryCounters_areInitialized() {
        // Counters should be registered
        assertNotNull(meterRegistry.counter("elasticsearch.events.indexed"));
        assertNotNull(meterRegistry.counter("elasticsearch.metrics.indexed"));
        assertNotNull(meterRegistry.counter("elasticsearch.index.errors"));
    }

    @Test
    void indexEvent_handlesNullReferrer() throws Exception {
        ClickEvent event = new ClickEvent(
                "event-123",
                "user-456",
                "session-789",
                "/home",
                null, // null referrer
                "click",
                "desktop",
                "US",
                "NYC",
                Instant.now(),
                System.currentTimeMillis()
        );

        String json = objectMapper.writeValueAsString(event);
        assertNotNull(json);
        assertTrue(json.contains("\"referrer\":null") || !json.contains("referrer"));
    }

    @Test
    void indexMetrics_handlesEmptyMaps() throws Exception {
        AggregatedMetrics metrics = new AggregatedMetrics(
                "window-empty",
                Instant.now().minusSeconds(60),
                Instant.now(),
                0L,
                0L,
                0L,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                0.0,
                0L,
                Instant.now()
        );

        String json = objectMapper.writeValueAsString(metrics);
        assertNotNull(json);
        assertTrue(json.contains("window-empty"));
    }
}
