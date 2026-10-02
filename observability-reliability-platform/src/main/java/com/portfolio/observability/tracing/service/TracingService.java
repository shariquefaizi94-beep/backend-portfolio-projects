package com.portfolio.observability.tracing.service;

import com.portfolio.observability.tracing.model.SpanKind;
import com.portfolio.observability.tracing.model.SpanStatus;
import com.portfolio.observability.tracing.model.TraceSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for distributed tracing management.
 * Provides span collection, trace assembly, and trace analysis capabilities.
 */
@Service
public class TracingService {

    private static final Logger log = LoggerFactory.getLogger(TracingService.class);

    private final Map<String, TraceSpan> spans = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> traceToSpans = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> serviceToSpans = new ConcurrentHashMap<>();

    // Span Management

    public TraceSpan recordSpan(TraceSpan span) {
        spans.put(span.spanId(), span);
        
        // Index by trace ID
        traceToSpans.computeIfAbsent(span.traceId(), k -> 
            Collections.synchronizedSet(new HashSet<>())
        ).add(span.spanId());
        
        // Index by service
        if (span.serviceName() != null) {
            serviceToSpans.computeIfAbsent(span.serviceName(), k -> 
                Collections.synchronizedSet(new HashSet<>())
            ).add(span.spanId());
        }
        
        log.debug("Recorded span: {} for trace: {} service: {}", 
                span.spanId(), span.traceId(), span.serviceName());
        return span;
    }

    public List<TraceSpan> recordSpans(List<TraceSpan> spanList) {
        spanList.forEach(this::recordSpan);
        return spanList;
    }

    public TraceSpan startSpan(String traceId, String operationName, String serviceName, SpanKind kind) {
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        TraceSpan span = TraceSpan.create(traceId, spanId, operationName, serviceName, kind);
        return recordSpan(span);
    }

    public TraceSpan startChildSpan(String traceId, String parentSpanId, String operationName, 
                                     String serviceName, SpanKind kind) {
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        TraceSpan span = TraceSpan.child(traceId, spanId, parentSpanId, operationName, serviceName, kind);
        return recordSpan(span);
    }

    public TraceSpan endSpan(String spanId) {
        TraceSpan span = spans.get(spanId);
        if (span == null) {
            throw new IllegalArgumentException("Span not found: " + spanId);
        }
        TraceSpan ended = span.end();
        spans.put(spanId, ended);
        return ended;
    }

    public TraceSpan endSpan(String spanId, SpanStatus status) {
        TraceSpan span = spans.get(spanId);
        if (span == null) {
            throw new IllegalArgumentException("Span not found: " + spanId);
        }
        TraceSpan ended = span.end(status);
        spans.put(spanId, ended);
        return ended;
    }

    // Span Queries

    public Optional<TraceSpan> getSpan(String spanId) {
        return Optional.ofNullable(spans.get(spanId));
    }

    public List<TraceSpan> getSpansByTraceId(String traceId) {
        Set<String> spanIds = traceToSpans.get(traceId);
        if (spanIds == null) {
            return Collections.emptyList();
        }
        return spanIds.stream()
                .map(spans::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TraceSpan::startTime))
                .collect(Collectors.toList());
    }

    public List<TraceSpan> getSpansByService(String serviceName) {
        Set<String> spanIds = serviceToSpans.get(serviceName);
        if (spanIds == null) {
            return Collections.emptyList();
        }
        return spanIds.stream()
                .map(spans::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TraceSpan::startTime).reversed())
                .collect(Collectors.toList());
    }

    public List<TraceSpan> getSpansByServiceAndTimeRange(String serviceName, Instant start, Instant end) {
        return getSpansByService(serviceName).stream()
                .filter(s -> !s.startTime().isBefore(start) && !s.startTime().isAfter(end))
                .collect(Collectors.toList());
    }

    // Trace Operations

    public Trace getTrace(String traceId) {
        List<TraceSpan> traceSpans = getSpansByTraceId(traceId);
        if (traceSpans.isEmpty()) {
            return null;
        }
        
        TraceSpan rootSpan = traceSpans.stream()
                .filter(TraceSpan::isRoot)
                .findFirst()
                .orElse(traceSpans.get(0));
        
        Duration totalDuration = calculateTraceDuration(traceSpans);
        int spanCount = traceSpans.size();
        Set<String> services = traceSpans.stream()
                .map(TraceSpan::serviceName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        boolean hasErrors = traceSpans.stream().anyMatch(TraceSpan::isError);
        
        return new Trace(traceId, rootSpan, traceSpans, totalDuration, spanCount, services, hasErrors);
    }

    public List<String> findTraceIds(Instant start, Instant end, int limit) {
        return spans.values().stream()
                .filter(s -> !s.startTime().isBefore(start) && !s.startTime().isAfter(end))
                .map(TraceSpan::traceId)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Trace> findTraces(Instant start, Instant end, int limit) {
        return findTraceIds(start, end, limit).stream()
                .map(this::getTrace)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Trace> findErrorTraces(Instant start, Instant end, int limit) {
        return findTraces(start, end, limit * 2).stream()
                .filter(Trace::hasErrors)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private Duration calculateTraceDuration(List<TraceSpan> traceSpans) {
        if (traceSpans.isEmpty()) {
            return Duration.ZERO;
        }
        
        Instant earliest = traceSpans.stream()
                .map(TraceSpan::startTime)
                .min(Comparator.naturalOrder())
                .orElse(Instant.now());
        
        Instant latest = traceSpans.stream()
                .map(s -> s.endTime() != null ? s.endTime() : s.startTime())
                .max(Comparator.naturalOrder())
                .orElse(Instant.now());
        
        return Duration.between(earliest, latest);
    }

    // Slow and Error Span Queries

    public List<TraceSpan> getSlowSpans(Duration threshold) {
        return spans.values().stream()
                .filter(s -> s.duration().compareTo(threshold) > 0)
                .sorted(Comparator.comparing(TraceSpan::duration).reversed())
                .collect(Collectors.toList());
    }

    public List<TraceSpan> getSlowSpansByService(String serviceName, Duration threshold) {
        return getSpansByService(serviceName).stream()
                .filter(s -> s.duration().compareTo(threshold) > 0)
                .sorted(Comparator.comparing(TraceSpan::duration).reversed())
                .collect(Collectors.toList());
    }

    public List<TraceSpan> getErrorSpans() {
        return spans.values().stream()
                .filter(TraceSpan::isError)
                .sorted(Comparator.comparing(TraceSpan::startTime).reversed())
                .collect(Collectors.toList());
    }

    public List<TraceSpan> getErrorSpansByService(String serviceName) {
        return getSpansByService(serviceName).stream()
                .filter(TraceSpan::isError)
                .collect(Collectors.toList());
    }

    // Child Span Queries

    public List<TraceSpan> getChildSpans(String parentSpanId) {
        return spans.values().stream()
                .filter(s -> parentSpanId.equals(s.parentSpanId()))
                .sorted(Comparator.comparing(TraceSpan::startTime))
                .collect(Collectors.toList());
    }

    // Service Discovery

    public List<String> getDistinctServices() {
        return new ArrayList<>(serviceToSpans.keySet());
    }

    public Map<String, Long> getSpanCountByService() {
        return serviceToSpans.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (long) e.getValue().size()
                ));
    }

    // Statistics

    public TracingStatistics getStatistics() {
        long totalSpans = spans.size();
        long totalTraces = traceToSpans.size();
        long errorSpans = spans.values().stream().filter(TraceSpan::isError).count();
        
        double avgLatency = spans.values().stream()
                .filter(s -> s.endTime() != null)
                .mapToLong(s -> s.duration().toMillis())
                .average()
                .orElse(0.0);
        
        double errorRate = totalSpans > 0 ? (double) errorSpans / totalSpans * 100.0 : 0.0;
        
        return new TracingStatistics(totalSpans, totalTraces, errorSpans, 
                serviceToSpans.size(), avgLatency, errorRate);
    }

    public ServiceTracingStatistics getServiceStatistics(String serviceName) {
        List<TraceSpan> serviceSpans = getSpansByService(serviceName);
        
        long totalSpans = serviceSpans.size();
        long errorSpans = serviceSpans.stream().filter(TraceSpan::isError).count();
        
        double avgLatency = serviceSpans.stream()
                .filter(s -> s.endTime() != null)
                .mapToLong(s -> s.duration().toMillis())
                .average()
                .orElse(0.0);
        
        double p50Latency = calculatePercentileLatency(serviceSpans, 50);
        double p95Latency = calculatePercentileLatency(serviceSpans, 95);
        double p99Latency = calculatePercentileLatency(serviceSpans, 99);
        
        double errorRate = totalSpans > 0 ? (double) errorSpans / totalSpans * 100.0 : 0.0;
        
        return new ServiceTracingStatistics(serviceName, totalSpans, errorSpans,
                avgLatency, p50Latency, p95Latency, p99Latency, errorRate);
    }

    private double calculatePercentileLatency(List<TraceSpan> serviceSpans, double percentile) {
        List<Long> durations = serviceSpans.stream()
                .filter(s -> s.endTime() != null)
                .map(s -> s.duration().toMillis())
                .sorted()
                .collect(Collectors.toList());
        
        if (durations.isEmpty()) {
            return 0.0;
        }
        
        int index = (int) Math.ceil(percentile / 100.0 * durations.size()) - 1;
        index = Math.max(0, Math.min(index, durations.size() - 1));
        return durations.get(index);
    }

    // Maintenance

    public void deleteTrace(String traceId) {
        Set<String> spanIds = traceToSpans.remove(traceId);
        if (spanIds != null) {
            spanIds.forEach(spanId -> {
                TraceSpan span = spans.remove(spanId);
                if (span != null && span.serviceName() != null) {
                    Set<String> serviceSpans = serviceToSpans.get(span.serviceName());
                    if (serviceSpans != null) {
                        serviceSpans.remove(spanId);
                    }
                }
            });
        }
    }

    public void pruneOldSpans(Duration retention) {
        Instant threshold = Instant.now().minus(retention);
        log.info("Pruning spans older than: {}", threshold);
        
        List<String> toDelete = spans.values().stream()
                .filter(s -> s.startTime().isBefore(threshold))
                .map(TraceSpan::spanId)
                .collect(Collectors.toList());
        
        for (String spanId : toDelete) {
            TraceSpan span = spans.remove(spanId);
            if (span != null) {
                Set<String> traceSpans = traceToSpans.get(span.traceId());
                if (traceSpans != null) {
                    traceSpans.remove(spanId);
                    if (traceSpans.isEmpty()) {
                        traceToSpans.remove(span.traceId());
                    }
                }
                if (span.serviceName() != null) {
                    Set<String> serviceSpans = serviceToSpans.get(span.serviceName());
                    if (serviceSpans != null) {
                        serviceSpans.remove(spanId);
                        if (serviceSpans.isEmpty()) {
                            serviceToSpans.remove(span.serviceName());
                        }
                    }
                }
            }
        }
    }

    public void clear() {
        spans.clear();
        traceToSpans.clear();
        serviceToSpans.clear();
    }

    // Inner types

    public record Trace(
            String traceId,
            TraceSpan rootSpan,
            List<TraceSpan> spans,
            Duration duration,
            int spanCount,
            Set<String> services,
            boolean hasErrors
    ) {}

    public record TracingStatistics(
            long totalSpans,
            long totalTraces,
            long errorSpans,
            int serviceCount,
            double averageLatencyMs,
            double errorRate
    ) {}

    public record ServiceTracingStatistics(
            String serviceName,
            long totalSpans,
            long errorSpans,
            double averageLatencyMs,
            double p50LatencyMs,
            double p95LatencyMs,
            double p99LatencyMs,
            double errorRate
    ) {}
}
