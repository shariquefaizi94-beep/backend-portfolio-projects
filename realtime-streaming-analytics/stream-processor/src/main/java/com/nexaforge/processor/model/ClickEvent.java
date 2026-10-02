package com.nexaforge.processor.model;

import java.time.Instant;

/**
 * Incoming click event from Kafka.
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
) {}
