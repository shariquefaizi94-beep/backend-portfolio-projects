package com.nexaforge.order.outbox;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox pattern entity.
 *
 * <p>Events are written to this table in the same transaction as the
 * domain state change. A background poller reads unsent events and
 * publishes them to Kafka, then marks them as sent.
 *
 * <p>This guarantees at-least-once delivery without requiring
 * distributed transactions (2PC) between the database and Kafka.
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_sent", columnList = "sent"),
        @Index(name = "idx_outbox_created_at", columnList = "createdAt")
})
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String aggregateType;

    @Column(nullable = false, length = 128)
    private String aggregateId;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 128)
    private String topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean sent;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant sentAt;

    protected OutboxEvent() {}

    public static OutboxEvent create(String aggregateType, String aggregateId,
                                     String eventType, String topic, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.topic = topic;
        event.payload = payload;
        event.sent = false;
        event.createdAt = Instant.now();
        return event;
    }

    public void markSent() {
        this.sent = true;
        this.sentAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getTopic() { return topic; }
    public String getPayload() { return payload; }
    public boolean isSent() { return sent; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
}
