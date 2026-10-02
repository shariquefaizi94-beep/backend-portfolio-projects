package com.nexaforge.processor.kafka;

import com.nexaforge.processor.aggregation.WindowedAggregator;
import com.nexaforge.processor.elasticsearch.ElasticsearchIndexer;
import com.nexaforge.processor.model.ClickEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-throughput Kafka consumer with:
 * - Redis-based deduplication
 * - Windowed aggregation
 * - Elasticsearch indexing
 *
 * <p>Uses batch consumption for better throughput.
 */
@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);
    private static final String DEDUP_PREFIX = "dedup:";

    private final WindowedAggregator aggregator;
    private final ElasticsearchIndexer esIndexer;
    private final StringRedisTemplate redisTemplate;
    private final Counter eventsProcessed;
    private final Counter duplicatesSkipped;
    private final AtomicLong totalProcessed = new AtomicLong(0);

    @Value("${deduplication.ttl-seconds:3600}")
    private int dedupTtlSeconds;

    @Value("${processor.index-raw-events:false}")
    private boolean indexRawEvents;

    public EventConsumer(WindowedAggregator aggregator,
                         ElasticsearchIndexer esIndexer,
                         StringRedisTemplate redisTemplate,
                         MeterRegistry meterRegistry) {
        this.aggregator = aggregator;
        this.esIndexer = esIndexer;
        this.redisTemplate = redisTemplate;
        this.eventsProcessed = Counter.builder("kafka.events.processed").register(meterRegistry);
        this.duplicatesSkipped = Counter.builder("kafka.events.duplicates").register(meterRegistry);
    }

    @KafkaListener(topics = "${kafka.topics.click-events}", groupId = "stream-processor",
            containerFactory = "batchFactory")
    public void processBatch(List<ClickEvent> events, Acknowledgment ack) {
        for (ClickEvent event : events) {
            if (isDuplicate(event.eventId())) {
                duplicatesSkipped.increment();
                continue;
            }

            // Add to windowed aggregation
            aggregator.addEvent(event);

            // Optionally index raw events to ES
            if (indexRawEvents) {
                esIndexer.indexEvent(event);
            }

            eventsProcessed.increment();
            totalProcessed.incrementAndGet();
        }

        ack.acknowledge();

        if (totalProcessed.get() % 5000 == 0) {
            log.info("Processed {} events, current window size: {}",
                    totalProcessed.get(), aggregator.getCurrentWindowSize());
        }
    }

    private boolean isDuplicate(String eventId) {
        String key = DEDUP_PREFIX + eventId;
        Boolean wasAbsent = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(dedupTtlSeconds));
        return !Boolean.TRUE.equals(wasAbsent);
    }

    public long getTotalProcessed() { return totalProcessed.get(); }
}
