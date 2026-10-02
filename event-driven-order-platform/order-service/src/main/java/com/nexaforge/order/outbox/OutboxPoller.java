package com.nexaforge.order.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Polls the outbox table for unsent events and publishes them to Kafka.
 *
 * <p>This implements the "polling publisher" variant of the transactional outbox pattern.
 * An alternative is CDC (Change Data Capture) using Debezium, which avoids polling
 * but adds infrastructure complexity.
 *
 * <p>Guarantees:
 * <ul>
 *   <li>Events are published in order of creation (FIFO within aggregate)</li>
 *   <li>At-least-once delivery — consumers must be idempotent</li>
 *   <li>No events lost — they're persisted in the same DB transaction as the domain change</li>
 * </ul>
 */
@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;
    private final Counter publishedCounter;
    private final Counter failedCounter;

    public OutboxPoller(
            OutboxRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${outbox.batch-size:50}") int batchSize,
            MeterRegistry meterRegistry) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
        this.publishedCounter = Counter.builder("outbox.events.published")
                .description("Number of outbox events published to Kafka")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("outbox.events.failed")
                .description("Number of outbox events that failed to publish")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:1000}")
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> events = outboxRepository.findUnsentEvents(
                PageRequest.of(0, batchSize));

        if (events.isEmpty()) return;

        log.debug("Polling outbox: found {} unsent events", events.size());

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish outbox event {} to {}",
                                        event.getId(), event.getTopic(), ex);
                                failedCounter.increment();
                            }
                        });
                event.markSent();
                publishedCounter.increment();
                log.debug("Published outbox event: {} → {}", event.getEventType(), event.getTopic());
            } catch (Exception e) {
                log.error("Failed to publish outbox event: {}", event.getId(), e);
                failedCounter.increment();
            }
        }

        outboxRepository.saveAll(events);
    }
}
