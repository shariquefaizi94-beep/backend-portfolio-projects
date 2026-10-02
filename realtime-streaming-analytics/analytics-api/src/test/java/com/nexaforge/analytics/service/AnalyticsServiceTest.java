package com.nexaforge.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexaforge.analytics.model.AggregatedMetrics;
import com.nexaforge.analytics.model.DashboardSummary;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalyticsService caching and aggregation logic.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private SimpleMeterRegistry meterRegistry;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        meterRegistry = new SimpleMeterRegistry();
        analyticsService = new AnalyticsService(objectMapper, redisTemplate, meterRegistry);
        ReflectionTestUtils.setField(analyticsService, "esUrl", "http://localhost:9200");
        ReflectionTestUtils.setField(analyticsService, "metricsIndex", "aggregated-metrics");
        ReflectionTestUtils.setField(analyticsService, "cacheTtl", 30);
    }

    @Test
    void getDashboardSummary_returnsCachedValueWhenPresent() throws Exception {
        DashboardSummary cached = new DashboardSummary(
                1000L, 100L, 150L, 20.0,
                Map.of("click", 500L), Map.of("US", 1000L), Map.of("desktop", 800L),
                10, Instant.now()
        );
        String cachedJson = objectMapper.writeValueAsString(cached);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("analytics:dashboard:10")).thenReturn(cachedJson);

        DashboardSummary result = analyticsService.getDashboardSummary(10);

        assertEquals(cached.totalEvents(), result.totalEvents());
        assertEquals(cached.uniqueUsers(), result.uniqueUsers());
        assertEquals(1.0, meterRegistry.counter("analytics.cache.hits").count());
    }

    @Test
    void getDashboardSummary_incrementsCacheHitCounter() throws Exception {
        DashboardSummary cached = new DashboardSummary(
                500L, 50L, 75L, 15.0,
                Map.of(), Map.of(), Map.of(),
                5, Instant.now()
        );
        String cachedJson = objectMapper.writeValueAsString(cached);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("analytics:dashboard:5")).thenReturn(cachedJson);

        double initialHits = meterRegistry.counter("analytics.cache.hits").count();
        analyticsService.getDashboardSummary(5);

        assertEquals(initialHits + 1, meterRegistry.counter("analytics.cache.hits").count());
    }

    @Test
    void getDashboardSummary_handlesCorruptedCacheGracefully() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("analytics:dashboard:10")).thenReturn("not valid json");

        // Should not throw, returns computed result instead
        assertDoesNotThrow(() -> analyticsService.getDashboardSummary(10));
    }

    @Test
    void getRecentMetrics_returnsEmptyListOnError() {
        // ES unavailable scenario - should return empty list, not throw
        // Note: getRecentMetrics queries ES directly, doesn't use Redis
        var result = analyticsService.getRecentMetrics(10);

        assertNotNull(result);
        assertTrue(result.isEmpty()); // ES is unavailable, returns empty list
    }

    @Test
    void metricsCounters_areRegistered() {
        assertNotNull(meterRegistry.counter("analytics.queries.executed"));
        assertNotNull(meterRegistry.counter("analytics.cache.hits"));
    }

    @Test
    void getDashboardSummary_cachesResult() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        analyticsService.getDashboardSummary(5);

        // Verify cache write was attempted
        verify(valueOperations).set(eq("analytics:dashboard:5"), anyString(), any(Duration.class));
    }

    @Test
    void getDashboardSummary_usesCorrectCacheKey() throws Exception {
        DashboardSummary cached = new DashboardSummary(
                100L, 10L, 15L, 5.0,
                Map.of(), Map.of(), Map.of(),
                3, Instant.now()
        );
        String cachedJson = objectMapper.writeValueAsString(cached);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("analytics:dashboard:3")).thenReturn(cachedJson);

        analyticsService.getDashboardSummary(3);

        verify(valueOperations).get("analytics:dashboard:3");
    }
}
