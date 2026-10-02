package com.nexaforge.producer.generator;

import com.nexaforge.producer.kafka.EventProducer;
import com.nexaforge.producer.model.ClickEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventGenerator batch generation and control.
 */
@ExtendWith(MockitoExtension.class)
class EventGeneratorTest {

    @Mock
    private EventProducer eventProducer;

    private SimpleMeterRegistry meterRegistry;
    private EventGenerator eventGenerator;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        eventGenerator = new EventGenerator(eventProducer, meterRegistry);
        ReflectionTestUtils.setField(eventGenerator, "eventsPerBatch", 10);
        ReflectionTestUtils.setField(eventGenerator, "userPoolSize", 100);
    }

    @Test
    void start_setsRunningToTrue() {
        assertFalse(eventGenerator.isRunning());

        eventGenerator.start();

        assertTrue(eventGenerator.isRunning());
    }

    @Test
    void stop_setsRunningToFalse() {
        eventGenerator.start();
        assertTrue(eventGenerator.isRunning());

        eventGenerator.stop();

        assertFalse(eventGenerator.isRunning());
    }

    @Test
    void generateBatch_doesNothingWhenNotRunning() {
        assertFalse(eventGenerator.isRunning());

        eventGenerator.generateBatch();

        verify(eventProducer, never()).send(any(ClickEvent.class));
        assertEquals(0, eventGenerator.getTotalGenerated());
    }

    @Test
    void generateBatch_sendsEventsWhenRunning() {
        eventGenerator.start();

        eventGenerator.generateBatch();

        verify(eventProducer, times(10)).send(any(ClickEvent.class));
        assertEquals(10, eventGenerator.getTotalGenerated());
    }

    @Test
    void generateBatch_incrementsMetrics() {
        eventGenerator.start();
        double initialCount = meterRegistry.counter("events.generated").count();

        eventGenerator.generateBatch();

        assertEquals(initialCount + 10, meterRegistry.counter("events.generated").count());
    }

    @Test
    void generateBatch_accumulatesTotal() {
        eventGenerator.start();

        eventGenerator.generateBatch();
        eventGenerator.generateBatch();
        eventGenerator.generateBatch();

        assertEquals(30, eventGenerator.getTotalGenerated());
        verify(eventProducer, times(30)).send(any(ClickEvent.class));
    }

    @Test
    void generateBatch_respectsEventsPerBatchConfig() {
        ReflectionTestUtils.setField(eventGenerator, "eventsPerBatch", 5);
        eventGenerator.start();

        eventGenerator.generateBatch();

        verify(eventProducer, times(5)).send(any(ClickEvent.class));
        assertEquals(5, eventGenerator.getTotalGenerated());
    }

    @Test
    void getTotalGenerated_returnsAccurateCount() {
        assertEquals(0, eventGenerator.getTotalGenerated());

        eventGenerator.start();
        eventGenerator.generateBatch();

        assertEquals(10, eventGenerator.getTotalGenerated());
    }

    @Test
    void stop_afterGeneration_showsCorrectTotal() {
        eventGenerator.start();
        eventGenerator.generateBatch();
        eventGenerator.generateBatch();

        eventGenerator.stop();

        assertFalse(eventGenerator.isRunning());
        assertEquals(20, eventGenerator.getTotalGenerated());
    }
}
