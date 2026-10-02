package com.portfolio.observability.metrics.repository;

import com.portfolio.observability.metrics.model.MetricDataPoint;
import com.portfolio.observability.metrics.model.MetricDefinition;
import com.portfolio.observability.metrics.model.MetricType;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for metrics storage and retrieval.
 * Implements time-series data storage with efficient querying capabilities.
 */
@Repository
public class MetricsRepository {

    private final Map<String, MetricDefinition> metricDefinitions = new ConcurrentHashMap<>();
    private final Map<String, List<MetricDataPoint>> metricDataPoints = new ConcurrentHashMap<>();

    // Metric Definition operations

    public MetricDefinition saveDefinition(MetricDefinition definition) {
        metricDefinitions.put(definition.name(), definition);
        return definition;
    }

    public Optional<MetricDefinition> findDefinitionByName(String name) {
        return Optional.ofNullable(metricDefinitions.get(name));
    }

    public List<MetricDefinition> findAllDefinitions() {
        return new ArrayList<>(metricDefinitions.values());
    }

    public List<MetricDefinition> findDefinitionsByType(MetricType type) {
        return metricDefinitions.values().stream()
                .filter(d -> d.type() == type)
                .collect(Collectors.toList());
    }

    public List<MetricDefinition> findDefinitionsByTag(String tagKey, String tagValue) {
        return metricDefinitions.values().stream()
                .filter(d -> d.tags() != null && tagValue.equals(d.tags().get(tagKey)))
                .collect(Collectors.toList());
    }

    public void deleteDefinition(String name) {
        metricDefinitions.remove(name);
        metricDataPoints.remove(name);
    }

    public boolean existsDefinition(String name) {
        return metricDefinitions.containsKey(name);
    }

    // Metric Data Point operations

    public MetricDataPoint saveDataPoint(MetricDataPoint dataPoint) {
        metricDataPoints.computeIfAbsent(dataPoint.metricName(), k -> 
            Collections.synchronizedList(new ArrayList<>())
        ).add(dataPoint);
        return dataPoint;
    }

    public List<MetricDataPoint> saveAllDataPoints(List<MetricDataPoint> dataPoints) {
        dataPoints.forEach(this::saveDataPoint);
        return dataPoints;
    }

    public List<MetricDataPoint> findDataPointsByMetricName(String metricName) {
        return new ArrayList<>(metricDataPoints.getOrDefault(metricName, Collections.emptyList()));
    }

    public List<MetricDataPoint> findDataPointsByMetricNameAndTimeRange(
            String metricName, Instant start, Instant end) {
        return metricDataPoints.getOrDefault(metricName, Collections.emptyList()).stream()
                .filter(dp -> !dp.timestamp().isBefore(start) && !dp.timestamp().isAfter(end))
                .sorted(Comparator.comparing(MetricDataPoint::timestamp))
                .collect(Collectors.toList());
    }

    public List<MetricDataPoint> findDataPointsByLabel(String labelKey, String labelValue, Instant start, Instant end) {
        return metricDataPoints.values().stream()
                .flatMap(List::stream)
                .filter(dp -> dp.labels() != null && labelValue.equals(dp.labels().get(labelKey)))
                .filter(dp -> !dp.timestamp().isBefore(start) && !dp.timestamp().isAfter(end))
                .sorted(Comparator.comparing(MetricDataPoint::timestamp))
                .collect(Collectors.toList());
    }

    public Optional<MetricDataPoint> findLatestDataPoint(String metricName) {
        return metricDataPoints.getOrDefault(metricName, Collections.emptyList()).stream()
                .max(Comparator.comparing(MetricDataPoint::timestamp));
    }

    public Map<String, Double> getLatestValuesForAllMetrics() {
        return metricDataPoints.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .max(Comparator.comparing(MetricDataPoint::timestamp))
                                .map(MetricDataPoint::value)
                                .orElse(0.0)
                ));
    }

    public double calculateAverage(String metricName, Instant start, Instant end) {
        return findDataPointsByMetricNameAndTimeRange(metricName, start, end).stream()
                .mapToDouble(MetricDataPoint::value)
                .average()
                .orElse(0.0);
    }

    public double calculatePercentile(String metricName, Instant start, Instant end, double percentile) {
        List<Double> values = findDataPointsByMetricNameAndTimeRange(metricName, start, end).stream()
                .map(MetricDataPoint::value)
                .sorted()
                .collect(Collectors.toList());
        
        if (values.isEmpty()) {
            return 0.0;
        }
        
        int index = (int) Math.ceil(percentile / 100.0 * values.size()) - 1;
        index = Math.max(0, Math.min(index, values.size() - 1));
        return values.get(index);
    }

    public void deleteDataPointsOlderThan(Instant threshold) {
        metricDataPoints.values().forEach(list -> 
            list.removeIf(dp -> dp.timestamp().isBefore(threshold))
        );
    }

    public long countDataPoints(String metricName) {
        return metricDataPoints.getOrDefault(metricName, Collections.emptyList()).size();
    }

    public void clear() {
        metricDefinitions.clear();
        metricDataPoints.clear();
    }
}
