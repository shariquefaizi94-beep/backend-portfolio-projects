package com.nexaforge.processor.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClickEvent record in stream-processor module.
 */
class ClickEventTest {

    @Test
    void recordCreation_withAllFields() {
        Instant now = Instant.now();
        ClickEvent event = new ClickEvent(
                "event-123",
                "user-456",
                "session-789",
                "/home",
                "https://google.com",
                "click",
                "desktop",
                "US",
                "NYC",
                now,
                1000L
        );

        assertEquals("event-123", event.eventId());
        assertEquals("user-456", event.userId());
        assertEquals("session-789", event.sessionId());
        assertEquals("/home", event.pageUrl());
        assertEquals("https://google.com", event.referrer());
        assertEquals("click", event.eventType());
        assertEquals("desktop", event.deviceType());
        assertEquals("US", event.country());
        assertEquals("NYC", event.city());
        assertEquals(now, event.timestamp());
        assertEquals(1000L, event.processingTime());
    }

    @Test
    void recordEquality_sameValues() {
        Instant now = Instant.now();
        ClickEvent event1 = new ClickEvent(
                "id-1", "user-1", "session-1", "/page", null,
                "view", "mobile", "UK", "London", now, 500L
        );
        ClickEvent event2 = new ClickEvent(
                "id-1", "user-1", "session-1", "/page", null,
                "view", "mobile", "UK", "London", now, 500L
        );

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void recordInequality_differentValues() {
        Instant now = Instant.now();
        ClickEvent event1 = new ClickEvent(
                "id-1", "user-1", "session-1", "/page", null,
                "view", "mobile", "UK", "London", now, 500L
        );
        ClickEvent event2 = new ClickEvent(
                "id-2", "user-1", "session-1", "/page", null,
                "view", "mobile", "UK", "London", now, 500L
        );

        assertNotEquals(event1, event2);
    }

    @Test
    void nullReferrer_isAllowed() {
        ClickEvent event = new ClickEvent(
                "id-1", "user-1", "session-1", "/page", null,
                "view", "mobile", "UK", "London", Instant.now(), 500L
        );

        assertNull(event.referrer());
    }
}
