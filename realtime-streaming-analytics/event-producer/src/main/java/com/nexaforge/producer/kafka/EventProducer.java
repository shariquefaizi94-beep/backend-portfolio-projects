package com.nexaforge.producer.kafka;

import com.nexaforge.producer.model.ClickEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes click events to Kafka with metrics tracking.
 * Uses userId as partition key to ensure ordering per user.
 */
@Component
public class EventProducer {

    private static final Logger log = LoggerFactory.getLogger(EventProducer.class);

    private final KafkaTemplate<String, ClickEvent> kafkaTemplate;
    private final Counter sentCounter;
    private final Counter failedCounter;

    @Value("${kafka.topics.click-events}")
    private String topic;

    public EventProducer(KafkaTemplate<String, ClickEvent> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.sentCounter = Counter.builder("kafka.events.sent").register(meterRegistry);
        this.failedCounter = Counter.builder("kafka.events.failed").register(meterRegistry);
    }

    public void send(ClickEvent event) {
        kafkaTemplate.send(topic, event.userId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        failedCounter.increment();
                        log.error("Failed to send event: {}", event.eventId(), ex);
                    } else {
                        sentCounter.increment();
                    }
                });
    }
}
