package com.nexaforge.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events in the platform.
 * Every event carries a unique ID, correlation ID for tracing across services,
 * and a timestamp for event-time processing.
 */
public abstract class BaseEvent {

    private UUID eventId;
    private String correlationId;
    private Instant timestamp;
    private int version;

    protected BaseEvent() {
        this.eventId = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.version = 1;
    }

    protected BaseEvent(String correlationId) {
        this();
        this.correlationId = correlationId;
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
