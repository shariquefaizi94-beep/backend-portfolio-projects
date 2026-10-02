package com.portfolio.observability.tracing.repository;

import com.portfolio.observability.tracing.model.SpanKind;
import com.portfolio.observability.tracing.model.SpanStatus;
import com.portfolio.observability.tracing.model.TraceSpan;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for distributed tracing data.
 * Stores spans and provides trace assembly and analysis capabilities.
 */
@Repository
public class TracingRepository {

    private final Map<String, TraceSpan> spans = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> traceToSpans = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> serviceToSpans = new ConcurrentHashMap<>();

    public TraceSpan save(TraceSpan span) {
        spans.put(span.spanId(), span);
        
        // Index by trace ID
        traceToSpans.computeIfAbsent(span.traceId(), k -> 
            Collections.synchronizedSet(new HashSet<>())
        ).add(span.spanId());
        
        // Index by service name
        if (span.serviceName() != null) {
            serviceToSpans.computeIfAbsent(span.serviceName(), k -> 
                Collections.synchronizedSet(new HashSet<>())
            ).add(span.spanId());
        }
        
        return span;
    }

    public List<TraceSpan> saveAll(List<TraceSpan> spanList) {
        spanList.forEach(this::save);
        return spanList;
    }

    public Optional<TraceSpan> findBySpanId(String spanId) {
        return Optional.ofNullable(spans.get(spanId));
    }

    public List<TraceSpan> findByTraceId(String traceId) {
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

    public List<TraceSpan> findByService(String serviceName) {
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

    public List<TraceSpan> findByServiceAndTimeRange(String serviceName, Instant start, Instant end) {
        return findByService(serviceName).stream()
                .filter(s -> !s.startTime().isBefore(start) && !s.startTime().isAfter(end))
                .collect(Collectors.toList());
    }

    public List<TraceSpan> findByStatus(SpanStatus status) {
        return spans.values().stream()
                .filter(s -> s.status() == status)
                .sorted(Comparator.comparing(TraceSpan::startTime).reversed())
                .collect(Collectors.toList());
    }

    public List<TraceSpan> findByKind(SpanKind kind) {
        return spans.values().stream()
                .filter(s -> s.kind() == kind)
                .sorted(Comparator.comparing(TraceSpan::startTime).reversed())
                .collect(Collectors.toList());
    }

    public List<TraceSpan> findErrorSpans() {
        return spans.values().stream()
                .filter(s -> s.status() == SpanStatus.ERROR)
                .sorted(Comparator.comparing(TraceSpan::startTime).reversed())
                .collect(Collectors.toList());
    }

    public List<TraceSpan> findSlowSpans(Duration threshold) {
        return spans.values().stream()
                .filter(s -> s.duration().compareTo(threshold) > 0)
                .sorted(Comparator.comparing(TraceSpan::duration).reversed())
                .collect(Collectors.toList());
    }

    public List<TraceSpan> findChildSpans(String parentSpanId) {
        return spans.values().stream()
                .filter(s -> parentSpanId.equals(s.parentSpanId()))
                .sorted(Comparator.comparing(TraceSpan::startTime))
                .collect(Collectors.toList());
    }

    public Optional<TraceSpan> findRootSpan(String traceId) {
        return findByTraceId(traceId).stream()
                .filter(s -> s.parentSpanId() == null)
                .findFirst();
    }

    public List<String> findDistinctTraceIds(Instant start, Instant end, int limit) {
        return spans.values().stream()
                .filter(s -> !s.startTime().isBefore(start) && !s.startTime().isAfter(end))
                .map(TraceSpan::traceId)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<String> findDistinctServices() {
        return new ArrayList<>(serviceToSpans.keySet());
    }

    public Map<String, Long> getSpanCountByService() {
        return serviceToSpans.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (long) e.getValue().size()
                ));
    }

    public Map<SpanStatus, Long> getSpanCountByStatus() {
        return spans.values().stream()
                .collect(Collectors.groupingBy(TraceSpan::status, Collectors.counting()));
    }

    public double calculateAverageLatency(String serviceName) {
        return findByService(serviceName).stream()
                .mapToLong(s -> s.duration().toMillis())
                .average()
                .orElse(0.0);
    }

    public double calculateErrorRate(String serviceName) {
        List<TraceSpan> serviceSpans = findByService(serviceName);
        if (serviceSpans.isEmpty()) {
            return 0.0;
        }
        long errorCount = serviceSpans.stream()
                .filter(s -> s.status() == SpanStatus.ERROR)
                .count();
        return (double) errorCount / serviceSpans.size() * 100.0;
    }

    public Duration calculatePercentileLatency(String serviceName, double percentile) {
        List<Long> durations = findByService(serviceName).stream()
                .map(s -> s.duration().toMillis())
                .sorted()
                .collect(Collectors.toList());
        
        if (durations.isEmpty()) {
            return Duration.ZERO;
        }
        
        int index = (int) Math.ceil(percentile / 100.0 * durations.size()) - 1;
        index = Math.max(0, Math.min(index, durations.size() - 1));
        return Duration.ofMillis(durations.get(index));
    }

    public void deleteByTraceId(String traceId) {
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

    public void deleteOlderThan(Instant threshold) {
        List<String> toDelete = spans.values().stream()
                .filter(s -> s.startTime().isBefore(threshold))
                .map(TraceSpan::spanId)
                .collect(Collectors.toList());
        
        toDelete.forEach(spanId -> {
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
        });
    }

    public long count() {
        return spans.size();
    }

    public long countTraces() {
        return traceToSpans.size();
    }

    public void clear() {
        spans.clear();
        traceToSpans.clear();
        serviceToSpans.clear();
    }
}
