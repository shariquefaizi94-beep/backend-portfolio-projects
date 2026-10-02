package com.nexaforge.processor.aggregation;

import com.nexaforge.processor.elasticsearch.ElasticsearchIndexer;
import com.nexaforge.processor.model.AggregatedMetrics;
import com.nexaforge.processor.model.ClickEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory windowed aggregation with periodic flush to Elasticsearch.
 *
 * <p>Implements tumbling windows — collects events for a fixed duration,
 * computes aggregations, flushes to ES, then resets.
 *
 * <p>Production alternatives: Kafka Streams, Flink, or ksqlDB for
 * stateful stream processing with exactly-once semantics.
 */
@Component
public class WindowedAggregator {

    private static final Logger log = LoggerFactory.getLogger(WindowedAggregator.class);

    private final ElasticsearchIndexer esIndexer;
    private final Counter windowsFlushed;

    @Value("${aggregation.window-size-seconds:60}")
    private int windowSizeSeconds;

    // Current window state
    private final List<ClickEvent> currentWindow = Collections.synchronizedList(new ArrayList<>());
    private volatile Instant windowStart = Instant.now();

    public WindowedAggregator(ElasticsearchIndexer esIndexer, MeterRegistry meterRegistry) {
        this.esIndexer = esIndexer;
        this.windowsFlushed = Counter.builder("aggregation.windows.flushed").register(meterRegistry);
    }

    public void addEvent(ClickEvent event) {
        currentWindow.add(event);
    }

    @Scheduled(fixedDelayString = "${aggregation.flush-interval-ms:60000}")
    public void flushWindow() {
        if (currentWindow.isEmpty()) return;

        List<ClickEvent> eventsToAggregate;
        Instant windowEnd = Instant.now();
        Instant startTime = windowStart;

        synchronized (currentWindow) {
            eventsToAggregate = new ArrayList<>(currentWindow);
            currentWindow.clear();
            windowStart = Instant.now();
        }

        AggregatedMetrics metrics = computeAggregations(eventsToAggregate, startTime, windowEnd);
        esIndexer.indexMetrics(metrics);
        windowsFlushed.increment();

        log.info("Window flushed: {} events → {} unique users, {} sessions",
                metrics.totalEvents(), metrics.uniqueUsers(), metrics.uniqueSessions());
    }

    private AggregatedMetrics computeAggregations(List<ClickEvent> events, Instant start, Instant end) {
        Set<String> uniqueUsers = new HashSet<>();
        Set<String> uniqueSessions = new HashSet<>();
        Map<String, Long> byType = new HashMap<>();
        Map<String, Long> byCountry = new HashMap<>();
        Map<String, Long> byDevice = new HashMap<>();
        Map<String, Long> byPage = new HashMap<>();
        long totalLatency = 0;
        long maxLatency = 0;

        for (ClickEvent e : events) {
            uniqueUsers.add(e.userId());
            uniqueSessions.add(e.sessionId());
            byType.merge(e.eventType(), 1L, Long::sum);
            byCountry.merge(e.country(), 1L, Long::sum);
            byDevice.merge(e.deviceType(), 1L, Long::sum);
            byPage.merge(e.pageUrl(), 1L, Long::sum);

            long latency = System.currentTimeMillis() - e.processingTime();
            totalLatency += latency;
            maxLatency = Math.max(maxLatency, latency);
        }

        Map<String, Long> topPages = byPage.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        String windowId = start.truncatedTo(ChronoUnit.MINUTES).toString().replace(":", "-");

        return new AggregatedMetrics(
                windowId, start, end,
                events.size(),
                uniqueUsers.size(),
                uniqueSessions.size(),
                byType, byCountry, byDevice, topPages,
                events.isEmpty() ? 0 : (double) totalLatency / events.size(),
                maxLatency,
                Instant.now()
        );
    }

    public int getCurrentWindowSize() { return currentWindow.size(); }
}
