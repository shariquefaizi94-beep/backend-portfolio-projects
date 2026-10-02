package com.nexaforge.processor.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaforge.processor.model.AggregatedMetrics;
import com.nexaforge.processor.model.ClickEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Indexes click events and aggregated metrics to Elasticsearch.
 *
 * <p>Uses simple HTTP client for demonstration. Production systems
 * would use the official Elasticsearch Java client with bulk indexing.
 */
@Component
public class ElasticsearchIndexer {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexer.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Counter eventsIndexed;
    private final Counter metricsIndexed;
    private final Counter indexErrors;

    @Value("${elasticsearch.url:http://localhost:9200}")
    private String esUrl;

    @Value("${elasticsearch.events-index:click-events}")
    private String eventsIndex;

    @Value("${elasticsearch.metrics-index:aggregated-metrics}")
    private String metricsIndex;

    public ElasticsearchIndexer(ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.eventsIndexed = Counter.builder("elasticsearch.events.indexed").register(meterRegistry);
        this.metricsIndexed = Counter.builder("elasticsearch.metrics.indexed").register(meterRegistry);
        this.indexErrors = Counter.builder("elasticsearch.index.errors").register(meterRegistry);
    }

    public void indexEvent(ClickEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(esUrl + "/" + eventsIndex + "/_doc/" + event.eventId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(resp -> {
                        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                            eventsIndexed.increment();
                        } else {
                            indexErrors.increment();
                            log.warn("ES index failed: {} - {}", resp.statusCode(), resp.body());
                        }
                    });
        } catch (Exception e) {
            indexErrors.increment();
            log.error("Failed to index event", e);
        }
    }

    public void indexMetrics(AggregatedMetrics metrics) {
        try {
            String json = objectMapper.writeValueAsString(metrics);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(esUrl + "/" + metricsIndex + "/_doc/" + metrics.windowId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(resp -> {
                        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                            metricsIndexed.increment();
                            log.info("Indexed metrics for window {}", metrics.windowId());
                        } else {
                            indexErrors.increment();
                            log.warn("ES metrics index failed: {} - {}", resp.statusCode(), resp.body());
                        }
                    });
        } catch (Exception e) {
            indexErrors.increment();
            log.error("Failed to index metrics", e);
        }
    }
}
