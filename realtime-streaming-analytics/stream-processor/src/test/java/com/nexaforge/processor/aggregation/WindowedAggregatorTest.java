package com.nexaforge.processor.aggregation;

import com.nexaforge.processor.elasticsearch.ElasticsearchIndexer;
import com.nexaforge.processor.model.AggregatedMetrics;
import com.nexaforge.processor.model.ClickEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WindowedAggregator tumbling window implementation.
 */
@ExtendWith(MockitoExtension.class)
class WindowedAggregatorTest {

    @Mock
    private ElasticsearchIndexer esIndexer;

    private SimpleMeterRegistry meterRegistry;
    private WindowedAggregator aggregator;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        aggregator = new WindowedAggregator(esIndexer, meterRegistry);
        ReflectionTestUtils.setField(aggregator, "windowSizeSeconds", 60);
    }

    @Test
    void addEvent_increasesWindowSize() {
        assertEquals(0, aggregator.getCurrentWindowSize());

        aggregator.addEvent(createTestEvent("user-1", "session-1", "click", "US", "desktop", "/home"));

        assertEquals(1, aggregator.getCurrentWindowSize());
    }

    @Test
    void addEvent_accumulatesMultipleEvents() {
        aggregator.addEvent(createTestEvent("user-1", "session-1", "click", "US", "desktop", "/home"));
        aggregator.addEvent(createTestEvent("user-2", "session-2", "view", "UK", "mobile", "/products"));
        aggregator.addEvent(createTestEvent("user-3", "session-3", "purchase", "DE", "tablet", "/checkout"));

        assertEquals(3, aggregator.getCurrentWindowSize());
    }

    @Test
    void flushWindow_doesNothingWhenEmpty() {
        aggregator.flushWindow();

        verify(esIndexer, never()).indexMetrics(any());
        assertEquals(0, meterRegistry.counter("aggregation.windows.flushed").count());
    }

    @Test
    void flushWindow_indexesAggregatedMetrics() {
        aggregator.addEvent(createTestEvent("user-1", "session-1", "click", "US", "desktop", "/home"));
        aggregator.addEvent(createTestEvent("user-2", "session-2", "view", "UK", "mobile", "/products"));

        aggregator.flushWindow();

        ArgumentCaptor<AggregatedMetrics> captor = ArgumentCaptor.forClass(AggregatedMetrics.class);
        verify(esIndexer).indexMetrics(captor.capture());

        AggregatedMetrics metrics = captor.getValue();
        assertEquals(2, metrics.totalEvents());
    }

    @Test
    void flushWindow_clearsCurrentWindow() {
        aggregator.addEvent(createTestEvent("user-1", "session-1", "click", "US", "desktop", "/home"));
        assertEquals(1, aggregator.getCurrentWindowSize());

        aggregator.flushWindow();

        assertEquals(0, aggregator.getCurrentWindowSize());
    }

    @Test
    void flushWindow_incrementsMetric() {
        aggregator.addEvent(createTestEvent("user-1", "session-1", "click", "US", "desktop", "/home"));
        double initialCount = meterRegistry.counter("aggregation.windows.flushed").count();

        aggregator.flushWindow();

        assertEquals(initialCount + 1, meterRegistry.counter("aggregation.windows.flushed").count());
    }

    @Test
    void flushWindow_computesUniqueUsers() {
        aggregator.addEvent(createTestEvent("user-1", "session-1", "click", "US", "desktop", "/home"));
        aggregator.addEvent(createTestEvent("user-1", "session-2", "view", "US", "desktop", "/products"));
        aggregator.addEvent(createTestEvent("user-2", "session-3", "click", "UK", "mobile", "/home"));

        aggregator.flushWindow();

        ArgumentCaptor<AggregatedMetrics> captor = ArgumentCaptor.forClass(AggregatedMetrics.class);
        verify(esIndexer).indexMetrics(captor.capture());

        AggregatedMetrics metrics = captor.getValue();
        assertEquals(2, metrics.uniqueUsers()); // user-1 and user-2
    }

    @Test
    void flushWindow_computesUniqueSessions() {
        aggregator.addEvent(createTestEvent("user-1", "session-1", "click", "US", "desktop", "/home"));
        aggregator.addEvent(createTestEvent("user-1", "session-1", "view", "US", "desktop", "/products"));
        aggregator.addEvent(createTestEvent("user-2", "session-2", "click", "UK", "mobile", "/home"));

        aggregator.flushWindow();

        ArgumentCaptor<AggregatedMetrics> captor = ArgumentCaptor.forClass(AggregatedMetrics.class);
        verify(esIndexer).indexMetrics(captor.capture());

        AggregatedMetrics metrics = captor.getValue();
        assertEquals(2, metrics.uniqueSessions()); // session-1 and session-2
    }

    @Test
    void flushWindow_computesEventsByType() {
        aggregator.addEvent(createTestEvent("user-1", "session-1", "click", "US", "desktop", "/home"));
        aggregator.addEvent(createTestEvent("user-2", "session-2", "click", "US", "desktop", "/home"));
        aggregator.addEvent(createTestEvent("user-3", "session-3", "view", "US", "desktop", "/home"));

        aggregator.flushWindow();

        ArgumentCaptor<AggregatedMetrics> captor = ArgumentCaptor.forClass(AggregatedMetrics.class);
        verify(esIndexer).indexMetrics(captor.capture());

        Map<String, Long> byType = captor.getValue().eventsByType();
        assertEquals(2L, byType.get("click"));
        assertEquals(1L, byType.get("view"));
    }

    @Test
    void flushWindow_computesEventsByCountry() {
        aggregator.addEvent(createTestEvent("user-1", "session-1", "click", "US", "desktop", "/home"));
        aggregator.addEvent(createTestEvent("user-2", "session-2", "click", "US", "desktop", "/home"));
        aggregator.addEvent(createTestEvent("user-3", "session-3", "view", "UK", "desktop", "/home"));

        aggregator.flushWindow();

        ArgumentCaptor<AggregatedMetrics> captor = ArgumentCaptor.forClass(AggregatedMetrics.class);
        verify(esIndexer).indexMetrics(captor.capture());

        Map<String, Long> byCountry = captor.getValue().eventsByCountry();
        assertEquals(2L, byCountry.get("US"));
        assertEquals(1L, byCountry.get("UK"));
    }

    @Test
    void flushWindow_computesEventsByDevice() {
        aggregator.addEvent(createTestEvent("user-1", "session-1", "click", "US", "desktop", "/home"));
        aggregator.addEvent(createTestEvent("user-2", "session-2", "click", "US", "mobile", "/home"));
        aggregator.addEvent(createTestEvent("user-3", "session-3", "view", "US", "mobile", "/home"));

        aggregator.flushWindow();

        ArgumentCaptor<AggregatedMetrics> captor = ArgumentCaptor.forClass(AggregatedMetrics.class);
        verify(esIndexer).indexMetrics(captor.capture());

        Map<String, Long> byDevice = captor.getValue().eventsByDevice();
        assertEquals(1L, byDevice.get("desktop"));
        assertEquals(2L, byDevice.get("mobile"));
    }

    @Test
    void flushWindow_computesTopPages() {
        aggregator.addEvent(createTestEvent("user-1", "session-1", "click", "US", "desktop", "/home"));
        aggregator.addEvent(createTestEvent("user-2", "session-2", "click", "US", "desktop", "/home"));
        aggregator.addEvent(createTestEvent("user-3", "session-3", "view", "US", "desktop", "/products"));

        aggregator.flushWindow();

        ArgumentCaptor<AggregatedMetrics> captor = ArgumentCaptor.forClass(AggregatedMetrics.class);
        verify(esIndexer).indexMetrics(captor.capture());

        Map<String, Long> topPages = captor.getValue().topPages();
        assertEquals(2L, topPages.get("/home"));
        assertEquals(1L, topPages.get("/products"));
    }

    private ClickEvent createTestEvent(String userId, String sessionId, String eventType,
                                        String country, String device, String page) {
        return new ClickEvent(
                "event-" + System.nanoTime(),
                userId,
                sessionId,
                page,
                null,
                eventType,
                device,
                country,
                "City",
                Instant.now(),
                System.currentTimeMillis()
        );
    }
}
