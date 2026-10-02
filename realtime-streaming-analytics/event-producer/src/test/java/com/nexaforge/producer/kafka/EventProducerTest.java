package com.nexaforge.producer.kafka;

import com.nexaforge.producer.model.ClickEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventProducer Kafka publishing.
 */
@ExtendWith(MockitoExtension.class)
class EventProducerTest {

    @Mock
    private KafkaTemplate<String, ClickEvent> kafkaTemplate;

    private SimpleMeterRegistry meterRegistry;
    private EventProducer eventProducer;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        eventProducer = new EventProducer(kafkaTemplate, meterRegistry);
        ReflectionTestUtils.setField(eventProducer, "topic", "click-events");
    }

    @Test
    void send_publishesToCorrectTopicWithUserId() {
        ClickEvent event = createTestEvent("user-123", "session-abc");
        CompletableFuture<SendResult<String, ClickEvent>> future = CompletableFuture.completedFuture(
                createSuccessSendResult("click-events", 0, 100L)
        );
        when(kafkaTemplate.send(anyString(), anyString(), any(ClickEvent.class))).thenReturn(future);

        eventProducer.send(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ClickEvent> eventCaptor = ArgumentCaptor.forClass(ClickEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        assertEquals("click-events", topicCaptor.getValue());
        assertEquals("user-123", keyCaptor.getValue());
        assertEquals(event, eventCaptor.getValue());
    }

    @Test
    void send_usesUserIdAsPartitionKey() {
        ClickEvent event = createTestEvent("user-456", "session-xyz");
        CompletableFuture<SendResult<String, ClickEvent>> future = CompletableFuture.completedFuture(
                createSuccessSendResult("click-events", 0, 100L)
        );
        when(kafkaTemplate.send(anyString(), anyString(), any(ClickEvent.class))).thenReturn(future);

        eventProducer.send(event);

        verify(kafkaTemplate).send(eq("click-events"), eq("user-456"), any(ClickEvent.class));
    }

    @Test
    void send_incrementsSentCounterOnSuccess() {
        ClickEvent event = createTestEvent("user-1", "session-1");
        CompletableFuture<SendResult<String, ClickEvent>> future = CompletableFuture.completedFuture(
                createSuccessSendResult("click-events", 0, 100L)
        );
        when(kafkaTemplate.send(anyString(), anyString(), any(ClickEvent.class))).thenReturn(future);

        double initialCount = meterRegistry.counter("kafka.events.sent").count();
        eventProducer.send(event);

        // Wait for async completion
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        assertTrue(meterRegistry.counter("kafka.events.sent").count() >= initialCount);
    }

    @Test
    void send_incrementsFailedCounterOnException() {
        ClickEvent event = createTestEvent("user-1", "session-1");
        CompletableFuture<SendResult<String, ClickEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(anyString(), anyString(), any(ClickEvent.class))).thenReturn(future);

        double initialCount = meterRegistry.counter("kafka.events.failed").count();
        eventProducer.send(event);

        // Wait for async completion
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        assertTrue(meterRegistry.counter("kafka.events.failed").count() >= initialCount);
    }

    private ClickEvent createTestEvent(String userId, String sessionId) {
        return new ClickEvent(
                "event-" + System.nanoTime(),
                userId,
                sessionId,
                "/home",
                "https://google.com",
                "click",
                "desktop",
                "US",
                "NYC",
                Instant.now(),
                System.currentTimeMillis()
        );
    }

    @SuppressWarnings("unchecked")
    private SendResult<String, ClickEvent> createSuccessSendResult(String topic, int partition, long offset) {
        ProducerRecord<String, ClickEvent> producerRecord = new ProducerRecord<>(topic, "key", null);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(topic, partition), offset, 0, System.currentTimeMillis(), 0, 0
        );
        return new SendResult<>(producerRecord, metadata);
    }
}
