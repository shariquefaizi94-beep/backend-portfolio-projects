package com.nexaforge.producer.generator;

import com.nexaforge.producer.kafka.EventProducer;
import com.nexaforge.producer.model.ClickEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates synthetic click events at configurable throughput.
 * Simulates realistic user sessions with multiple events per session.
 *
 * <p>Supports start/stop control via REST API for benchmarking.
 */
@Component
public class EventGenerator {

    private static final Logger log = LoggerFactory.getLogger(EventGenerator.class);

    private final EventProducer eventProducer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong totalGenerated = new AtomicLong(0);
    private final Counter eventsGenerated;

    @Value("${generator.events-per-batch:100}")
    private int eventsPerBatch;

    @Value("${generator.user-pool-size:1000}")
    private int userPoolSize;

    public EventGenerator(EventProducer eventProducer, MeterRegistry meterRegistry) {
        this.eventProducer = eventProducer;
        this.eventsGenerated = Counter.builder("events.generated")
                .description("Total synthetic events generated")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${generator.interval-ms:100}")
    public void generateBatch() {
        if (!running.get()) return;

        for (int i = 0; i < eventsPerBatch; i++) {
            String userId = "user-" + ((int) (Math.random() * userPoolSize));
            String sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);
            ClickEvent event = ClickEvent.generate(userId, sessionId);
            eventProducer.send(event);
            eventsGenerated.increment();
            totalGenerated.incrementAndGet();
        }

        if (totalGenerated.get() % 10000 == 0) {
            log.info("Generated {} events total", totalGenerated.get());
        }
    }

    public void start() {
        running.set(true);
        log.info("Event generation started ({} events/batch)", eventsPerBatch);
    }

    public void stop() {
        running.set(false);
        log.info("Event generation stopped. Total: {}", totalGenerated.get());
    }

    public boolean isRunning() { return running.get(); }
    public long getTotalGenerated() { return totalGenerated.get(); }
}
