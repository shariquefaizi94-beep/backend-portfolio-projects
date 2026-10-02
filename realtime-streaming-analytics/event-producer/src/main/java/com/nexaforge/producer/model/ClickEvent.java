package com.nexaforge.producer.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a user click/interaction event for analytics tracking.
 * Used to simulate clickstream data at high throughput.
 */
public record ClickEvent(
        String eventId,
        String userId,
        String sessionId,
        String pageUrl,
        String referrer,
        String eventType,
        String deviceType,
        String country,
        String city,
        Instant timestamp,
        long processingTime
) {
    public static ClickEvent generate(String userId, String sessionId) {
        String[] eventTypes = {"page_view", "click", "scroll", "form_submit", "purchase"};
        String[] devices = {"desktop", "mobile", "tablet"};
        String[] countries = {"US", "UK", "DE", "FR", "JP", "IN", "BR", "AU"};
        String[] pages = {"/home", "/products", "/cart", "/checkout", "/profile", "/search"};

        return new ClickEvent(
                UUID.randomUUID().toString(),
                userId,
                sessionId,
                pages[(int) (Math.random() * pages.length)],
                Math.random() > 0.3 ? "https://google.com" : null,
                eventTypes[(int) (Math.random() * eventTypes.length)],
                devices[(int) (Math.random() * devices.length)],
                countries[(int) (Math.random() * countries.length)],
                "City-" + (int) (Math.random() * 100),
                Instant.now(),
                System.currentTimeMillis()
        );
    }
}
