package com.portfolio.observability.tracing.model;

/**
 * Kind of span in a distributed trace.
 */
public enum SpanKind {
    SERVER,     // Server receiving a request
    CLIENT,     // Client making a request
    PRODUCER,   // Message producer
    CONSUMER,   // Message consumer
    INTERNAL    // Internal span
}
