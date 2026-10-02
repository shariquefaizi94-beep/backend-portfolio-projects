package com.nexaforge.producer.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClickEvent record and its generator.
 */
class ClickEventTest {

    @Test
    void generate_createsValidEvent() {
        String userId = "user-123";
        String sessionId = "session-abc";

        ClickEvent event = ClickEvent.generate(userId, sessionId);

        assertNotNull(event.eventId());
        assertEquals(userId, event.userId());
        assertEquals(sessionId, event.sessionId());
        assertNotNull(event.pageUrl());
        assertNotNull(event.eventType());
        assertNotNull(event.deviceType());
        assertNotNull(event.country());
        assertNotNull(event.city());
        assertNotNull(event.timestamp());
        assertTrue(event.processingTime() > 0);
    }

    @Test
    void generate_producesValidEventTypes() {
        Set<String> validTypes = Set.of("page_view", "click", "scroll", "form_submit", "purchase");

        for (int i = 0; i < 100; i++) {
            ClickEvent event = ClickEvent.generate("user-1", "session-1");
            assertTrue(validTypes.contains(event.eventType()),
                    "Unexpected event type: " + event.eventType());
        }
    }

    @Test
    void generate_producesValidDeviceTypes() {
        Set<String> validDevices = Set.of("desktop", "mobile", "tablet");

        for (int i = 0; i < 100; i++) {
            ClickEvent event = ClickEvent.generate("user-1", "session-1");
            assertTrue(validDevices.contains(event.deviceType()),
                    "Unexpected device type: " + event.deviceType());
        }
    }

    @Test
    void generate_producesValidCountries() {
        Set<String> validCountries = Set.of("US", "UK", "DE", "FR", "JP", "IN", "BR", "AU");

        for (int i = 0; i < 100; i++) {
            ClickEvent event = ClickEvent.generate("user-1", "session-1");
            assertTrue(validCountries.contains(event.country()),
                    "Unexpected country: " + event.country());
        }
    }

    @Test
    void generate_producesValidPages() {
        Set<String> validPages = Set.of("/home", "/products", "/cart", "/checkout", "/profile", "/search");

        for (int i = 0; i < 100; i++) {
            ClickEvent event = ClickEvent.generate("user-1", "session-1");
            assertTrue(validPages.contains(event.pageUrl()),
                    "Unexpected page: " + event.pageUrl());
        }
    }

    @Test
    void generate_producesUniqueEventIds() {
        Set<String> ids = new java.util.HashSet<>();

        for (int i = 0; i < 1000; i++) {
            ClickEvent event = ClickEvent.generate("user-1", "session-1");
            assertTrue(ids.add(event.eventId()), "Duplicate event ID generated");
        }
    }

    @Test
    void generate_timestampIsRecent() {
        Instant before = Instant.now();
        ClickEvent event = ClickEvent.generate("user-1", "session-1");
        Instant after = Instant.now();

        assertTrue(event.timestamp().isAfter(before.minusSeconds(1)));
        assertTrue(event.timestamp().isBefore(after.plusSeconds(1)));
    }

    @Test
    void recordEquality_works() {
        ClickEvent event1 = new ClickEvent(
                "id-1", "user-1", "session-1", "/home", null,
                "click", "desktop", "US", "NYC", Instant.now(), 1000L
        );
        ClickEvent event2 = new ClickEvent(
                "id-1", "user-1", "session-1", "/home", null,
                "click", "desktop", "US", "NYC", event1.timestamp(), 1000L
        );

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }
}
