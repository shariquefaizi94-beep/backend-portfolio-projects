package com.portfolio.observability.tracing.model;

/**
 * Status of a trace span.
 */
public enum SpanStatus {
    OK,
    ERROR,
    TIMEOUT,
    CANCELLED
}
