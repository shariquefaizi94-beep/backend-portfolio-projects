package com.portfolio.observability.tracing.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Represents a span in a distributed trace.
 */
public record TraceSpan(
    String traceId,
    String spanId,
    String parentSpanId,
    String operationName,
    String serviceName,
    SpanKind kind,
    SpanStatus status,
    Map<String, String> tags,
    Map<String, Object> baggage,
    Instant startTime,
    Instant endTime
) {
    
    public TraceSpan {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("Trace ID cannot be null or blank");
        }
        if (spanId == null || spanId.isBlank()) {
            throw new IllegalArgumentException("Span ID cannot be null or blank");
        }
        if (operationName == null || operationName.isBlank()) {
            throw new IllegalArgumentException("Operation name cannot be null or blank");
        }
    }
    
    public static TraceSpan create(String traceId, String spanId, String operationName, 
                                    String serviceName, SpanKind kind) {
        return new TraceSpan(
            traceId,
            spanId,
            null,
            operationName,
            serviceName,
            kind,
            SpanStatus.OK,
            Map.of(),
            Map.of(),
            Instant.now(),
            null
        );
    }
    
    public static TraceSpan child(String traceId, String spanId, String parentSpanId, 
                                   String operationName, String serviceName, SpanKind kind) {
        return new TraceSpan(
            traceId,
            spanId,
            parentSpanId,
            operationName,
            serviceName,
            kind,
            SpanStatus.OK,
            Map.of(),
            Map.of(),
            Instant.now(),
            null
        );
    }
    
    public TraceSpan end() {
        return new TraceSpan(traceId, spanId, parentSpanId, operationName, serviceName, 
            kind, status, tags, baggage, startTime, Instant.now());
    }
    
    public TraceSpan end(SpanStatus newStatus) {
        return new TraceSpan(traceId, spanId, parentSpanId, operationName, serviceName, 
            kind, newStatus, tags, baggage, startTime, Instant.now());
    }
    
    public TraceSpan withTags(Map<String, String> newTags) {
        return new TraceSpan(traceId, spanId, parentSpanId, operationName, serviceName, 
            kind, status, newTags, baggage, startTime, endTime);
    }
    
    public Duration duration() {
        if (endTime == null) {
            return Duration.between(startTime, Instant.now());
        }
        return Duration.between(startTime, endTime);
    }
    
    public boolean isRoot() {
        return parentSpanId == null;
    }
    
    public boolean isError() {
        return status == SpanStatus.ERROR || status == SpanStatus.TIMEOUT;
    }
}
