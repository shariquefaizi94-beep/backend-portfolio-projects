package com.portfolio.observability.tracing.controller;

import com.portfolio.observability.tracing.model.SpanKind;
import com.portfolio.observability.tracing.model.SpanStatus;
import com.portfolio.observability.tracing.model.TraceSpan;
import com.portfolio.observability.tracing.service.TracingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * REST controller for distributed tracing management.
 * Provides endpoints for span recording, trace querying, and trace analysis.
 */
@RestController
@RequestMapping("/api/v1/traces")
public class TracingController {

    private final TracingService tracingService;

    public TracingController(TracingService tracingService) {
        this.tracingService = tracingService;
    }

    // Span Recording Endpoints

    @PostMapping("/spans")
    public ResponseEntity<TraceSpan> recordSpan(@RequestBody TraceSpan span) {
        TraceSpan recorded = tracingService.recordSpan(span);
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }

    @PostMapping("/spans/batch")
    public ResponseEntity<List<TraceSpan>> recordSpans(@RequestBody List<TraceSpan> spans) {
        List<TraceSpan> recorded = tracingService.recordSpans(spans);
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }

    @PostMapping("/spans/start")
    public ResponseEntity<TraceSpan> startSpan(@RequestBody StartSpanRequest request) {
        TraceSpan span = tracingService.startSpan(
                request.traceId(), request.operationName(), 
                request.serviceName(), request.kind());
        return ResponseEntity.status(HttpStatus.CREATED).body(span);
    }

    @PostMapping("/spans/start-child")
    public ResponseEntity<TraceSpan> startChildSpan(@RequestBody StartChildSpanRequest request) {
        TraceSpan span = tracingService.startChildSpan(
                request.traceId(), request.parentSpanId(), request.operationName(),
                request.serviceName(), request.kind());
        return ResponseEntity.status(HttpStatus.CREATED).body(span);
    }

    @PostMapping("/spans/{spanId}/end")
    public ResponseEntity<TraceSpan> endSpan(
            @PathVariable String spanId,
            @RequestParam(required = false) SpanStatus status) {
        try {
            TraceSpan ended = status != null 
                    ? tracingService.endSpan(spanId, status)
                    : tracingService.endSpan(spanId);
            return ResponseEntity.ok(ended);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Span Query Endpoints

    @GetMapping("/spans/{spanId}")
    public ResponseEntity<TraceSpan> getSpan(@PathVariable String spanId) {
        return tracingService.getSpan(spanId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{traceId}/spans")
    public ResponseEntity<List<TraceSpan>> getSpansByTraceId(@PathVariable String traceId) {
        return ResponseEntity.ok(tracingService.getSpansByTraceId(traceId));
    }

    @GetMapping("/spans/service/{serviceName}")
    public ResponseEntity<List<TraceSpan>> getSpansByService(@PathVariable String serviceName) {
        return ResponseEntity.ok(tracingService.getSpansByService(serviceName));
    }

    @GetMapping("/spans/service/{serviceName}/range")
    public ResponseEntity<List<TraceSpan>> getSpansByServiceAndTimeRange(
            @PathVariable String serviceName,
            @RequestParam Instant start,
            @RequestParam Instant end) {
        return ResponseEntity.ok(tracingService.getSpansByServiceAndTimeRange(serviceName, start, end));
    }

    @GetMapping("/spans/{parentSpanId}/children")
    public ResponseEntity<List<TraceSpan>> getChildSpans(@PathVariable String parentSpanId) {
        return ResponseEntity.ok(tracingService.getChildSpans(parentSpanId));
    }

    // Trace Endpoints

    @GetMapping("/{traceId}")
    public ResponseEntity<TracingService.Trace> getTrace(@PathVariable String traceId) {
        TracingService.Trace trace = tracingService.getTrace(traceId);
        if (trace == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(trace);
    }

    @GetMapping
    public ResponseEntity<List<String>> findTraceIds(
            @RequestParam Instant start,
            @RequestParam Instant end,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(tracingService.findTraceIds(start, end, limit));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TracingService.Trace>> findTraces(
            @RequestParam Instant start,
            @RequestParam Instant end,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(tracingService.findTraces(start, end, limit));
    }

    @GetMapping("/errors")
    public ResponseEntity<List<TracingService.Trace>> findErrorTraces(
            @RequestParam Instant start,
            @RequestParam Instant end,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(tracingService.findErrorTraces(start, end, limit));
    }

    @DeleteMapping("/{traceId}")
    public ResponseEntity<Void> deleteTrace(@PathVariable String traceId) {
        tracingService.deleteTrace(traceId);
        return ResponseEntity.noContent().build();
    }

    // Slow and Error Span Queries

    @GetMapping("/spans/slow")
    public ResponseEntity<List<TraceSpan>> getSlowSpans(
            @RequestParam Duration threshold) {
        return ResponseEntity.ok(tracingService.getSlowSpans(threshold));
    }

    @GetMapping("/spans/slow/service/{serviceName}")
    public ResponseEntity<List<TraceSpan>> getSlowSpansByService(
            @PathVariable String serviceName,
            @RequestParam Duration threshold) {
        return ResponseEntity.ok(tracingService.getSlowSpansByService(serviceName, threshold));
    }

    @GetMapping("/spans/errors")
    public ResponseEntity<List<TraceSpan>> getErrorSpans() {
        return ResponseEntity.ok(tracingService.getErrorSpans());
    }

    @GetMapping("/spans/errors/service/{serviceName}")
    public ResponseEntity<List<TraceSpan>> getErrorSpansByService(@PathVariable String serviceName) {
        return ResponseEntity.ok(tracingService.getErrorSpansByService(serviceName));
    }

    // Service Discovery

    @GetMapping("/services")
    public ResponseEntity<List<String>> getDistinctServices() {
        return ResponseEntity.ok(tracingService.getDistinctServices());
    }

    // Statistics

    @GetMapping("/statistics")
    public ResponseEntity<TracingService.TracingStatistics> getStatistics() {
        return ResponseEntity.ok(tracingService.getStatistics());
    }

    @GetMapping("/statistics/service/{serviceName}")
    public ResponseEntity<TracingService.ServiceTracingStatistics> getServiceStatistics(
            @PathVariable String serviceName) {
        return ResponseEntity.ok(tracingService.getServiceStatistics(serviceName));
    }

    // Maintenance

    @DeleteMapping("/spans/prune")
    public ResponseEntity<Void> pruneOldSpans(@RequestParam Duration retention) {
        tracingService.pruneOldSpans(retention);
        return ResponseEntity.noContent().build();
    }

    // Request DTOs

    public record StartSpanRequest(
            String traceId,
            String operationName,
            String serviceName,
            SpanKind kind
    ) {}

    public record StartChildSpanRequest(
            String traceId,
            String parentSpanId,
            String operationName,
            String serviceName,
            SpanKind kind
    ) {}
}
