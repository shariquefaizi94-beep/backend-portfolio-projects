package com.nexaforge.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaforge.analytics.model.AggregatedMetrics;
import com.nexaforge.analytics.model.DashboardSummary;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Queries Elasticsearch for aggregated metrics.
 * Uses Redis caching to reduce ES load for frequently accessed queries.
 */
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static final String CACHE_PREFIX = "analytics:";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final StringRedisTemplate redisTemplate;
    private final Counter queriesExecuted;
    private final Counter cacheHits;

    @Value("${elasticsearch.url:http://localhost:9200}")
    private String esUrl;

    @Value("${elasticsearch.metrics-index:aggregated-metrics}")
    private String metricsIndex;

    @Value("${cache.ttl-seconds:30}")
    private int cacheTtl;

    public AnalyticsService(ObjectMapper objectMapper, StringRedisTemplate redisTemplate,
                            MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.queriesExecuted = Counter.builder("analytics.queries.executed").register(meterRegistry);
        this.cacheHits = Counter.builder("analytics.cache.hits").register(meterRegistry);
    }

    public DashboardSummary getDashboardSummary(int windowsToAggregate) {
        String cacheKey = CACHE_PREFIX + "dashboard:" + windowsToAggregate;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            cacheHits.increment();
            try {
                return objectMapper.readValue(cached, DashboardSummary.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached summary", e);
            }
        }

        queriesExecuted.increment();
        List<AggregatedMetrics> recent = getRecentMetrics(windowsToAggregate);

        long totalEvents = recent.stream().mapToLong(AggregatedMetrics::totalEvents).sum();
        long uniqueUsers = recent.stream().map(AggregatedMetrics::uniqueUsers).reduce(0L, Long::sum);
        long uniqueSessions = recent.stream().map(AggregatedMetrics::uniqueSessions).reduce(0L, Long::sum);
        double avgLatency = recent.stream().mapToDouble(AggregatedMetrics::avgLatencyMs).average().orElse(0);

        Map<String, Long> byType = new HashMap<>();
        Map<String, Long> byCountry = new HashMap<>();
        Map<String, Long> byDevice = new HashMap<>();
        for (AggregatedMetrics m : recent) {
            if (m.eventsByType() != null) m.eventsByType().forEach((k, v) -> byType.merge(k, v, Long::sum));
            if (m.eventsByCountry() != null) m.eventsByCountry().forEach((k, v) -> byCountry.merge(k, v, Long::sum));
            if (m.eventsByDevice() != null) m.eventsByDevice().forEach((k, v) -> byDevice.merge(k, v, Long::sum));
        }

        DashboardSummary summary = new DashboardSummary(
                totalEvents, uniqueUsers, uniqueSessions, avgLatency,
                byType, byCountry, byDevice, windowsToAggregate, Instant.now()
        );

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(summary),
                    Duration.ofSeconds(cacheTtl));
        } catch (Exception e) {
            log.warn("Failed to cache summary", e);
        }

        return summary;
    }

    public List<AggregatedMetrics> getRecentMetrics(int count) {
        try {
            String query = """
                {"size":%d,"sort":[{"windowEnd":{"order":"desc"}}]}
                """.formatted(count);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(esUrl + "/" + metricsIndex + "/_search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(query))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("ES query failed: {}", response.body());
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode hits = root.path("hits").path("hits");
            List<AggregatedMetrics> results = new ArrayList<>();
            for (JsonNode hit : hits) {
                results.add(objectMapper.treeToValue(hit.path("_source"), AggregatedMetrics.class));
            }
            return results;
        } catch (Exception e) {
            log.error("Failed to query ES", e);
            return Collections.emptyList();
        }
    }
}
