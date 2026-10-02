package com.portfolio.observability.metrics.service;

import com.portfolio.observability.metrics.model.MetricDataPoint;
import com.portfolio.observability.metrics.model.MetricDefinition;
import com.portfolio.observability.metrics.model.MetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing metrics collection, storage, and querying.
 * Provides comprehensive metrics capabilities for observability.
 */
@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final Map<String, MetricDefinition> metricDefinitions = new ConcurrentHashMap<>();
    private final Map<String, List<MetricDataPoint>> metricDataPoints = new ConcurrentHashMap<>();

    // Metric Definition Management

    public MetricDefinition registerMetric(MetricDefinition definition) {
        log.info("Registering metric: {} of type {}", definition.name(), definition.type());
        metricDefinitions.put(definition.name(), definition);
        return definition;
    }

    public Optional<MetricDefinition> getMetricDefinition(String name) {
        return Optional.ofNullable(metricDefinitions.get(name));
    }

    public List<MetricDefinition> getAllMetricDefinitions() {
        return new ArrayList<>(metricDefinitions.values());
    }

    public List<MetricDefinition> getMetricsByType(MetricType type) {
        return metricDefinitions.values().stream()
                .filter(d -> d.type() == type)
                .collect(Collectors.toList());
    }

    public void unregisterMetric(String name) {
        log.info("Unregistering metric: {}", name);
        metricDefinitions.remove(name);
        metricDataPoints.remove(name);
    }

    public boolean isMetricRegistered(String name) {
        return metricDefinitions.containsKey(name);
    }

    // Data Point Recording

    public MetricDataPoint recordDataPoint(MetricDataPoint dataPoint) {
        if (!metricDefinitions.containsKey(dataPoint.metricName())) {
            log.warn("Recording data point for unregistered metric: {}", dataPoint.metricName());
        }
        
        metricDataPoints.computeIfAbsent(dataPoint.metricName(), k -> 
            Collections.synchronizedList(new ArrayList<>())
        ).add(dataPoint);
        
        log.debug("Recorded data point for metric: {} value: {}", 
                dataPoint.metricName(), dataPoint.value());
        return dataPoint;
    }

    public List<MetricDataPoint> recordDataPoints(List<MetricDataPoint> dataPoints) {
        dataPoints.forEach(this::recordDataPoint);
        return dataPoints;
    }

    public MetricDataPoint recordValue(String metricName, double value) {
        return recordDataPoint(MetricDataPoint.of(metricName, value));
    }

    public MetricDataPoint recordValue(String metricName, double value, Map<String, String> labels) {
        return recordDataPoint(MetricDataPoint.of(metricName, value, labels));
    }

    // Counter-specific operations

    public void incrementCounter(String metricName) {
        incrementCounter(metricName, 1.0, Map.of());
    }

    public void incrementCounter(String metricName, double amount, Map<String, String> labels) {
        Optional<MetricDataPoint> latest = getLatestDataPoint(metricName);
        double currentValue = latest.map(MetricDataPoint::value).orElse(0.0);
        recordDataPoint(MetricDataPoint.of(metricName, currentValue + amount, labels));
    }

    // Data Point Querying

    public List<MetricDataPoint> getDataPoints(String metricName) {
        return new ArrayList<>(metricDataPoints.getOrDefault(metricName, Collections.emptyList()));
    }

    public List<MetricDataPoint> getDataPointsInRange(String metricName, Instant start, Instant end) {
        return metricDataPoints.getOrDefault(metricName, Collections.emptyList()).stream()
                .filter(dp -> !dp.timestamp().isBefore(start) && !dp.timestamp().isAfter(end))
                .sorted(Comparator.comparing(MetricDataPoint::timestamp))
                .collect(Collectors.toList());
    }

    public Optional<MetricDataPoint> getLatestDataPoint(String metricName) {
        return metricDataPoints.getOrDefault(metricName, Collections.emptyList()).stream()
                .max(Comparator.comparing(MetricDataPoint::timestamp));
    }

    public Map<String, Double> getLatestValues() {
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

    // Aggregation Functions

    public double calculateAverage(String metricName, Duration window) {
        Instant start = Instant.now().minus(window);
        return getDataPointsInRange(metricName, start, Instant.now()).stream()
                .mapToDouble(MetricDataPoint::value)
                .average()
                .orElse(0.0);
    }

    public double calculateSum(String metricName, Duration window) {
        Instant start = Instant.now().minus(window);
        return getDataPointsInRange(metricName, start, Instant.now()).stream()
                .mapToDouble(MetricDataPoint::value)
                .sum();
    }

    public double calculateMin(String metricName, Duration window) {
        Instant start = Instant.now().minus(window);
        return getDataPointsInRange(metricName, start, Instant.now()).stream()
                .mapToDouble(MetricDataPoint::value)
                .min()
                .orElse(0.0);
    }

    public double calculateMax(String metricName, Duration window) {
        Instant start = Instant.now().minus(window);
        return getDataPointsInRange(metricName, start, Instant.now()).stream()
                .mapToDouble(MetricDataPoint::value)
                .max()
                .orElse(0.0);
    }

    public double calculatePercentile(String metricName, Duration window, double percentile) {
        Instant start = Instant.now().minus(window);
        List<Double> values = getDataPointsInRange(metricName, start, Instant.now()).stream()
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

    public double calculateRate(String metricName, Duration window) {
        Instant start = Instant.now().minus(window);
        List<MetricDataPoint> points = getDataPointsInRange(metricName, start, Instant.now());
        
        if (points.size() < 2) {
            return 0.0;
        }
        
        MetricDataPoint first = points.get(0);
        MetricDataPoint last = points.get(points.size() - 1);
        
        double valueDiff = last.value() - first.value();
        long timeDiffSeconds = Duration.between(first.timestamp(), last.timestamp()).getSeconds();
        
        return timeDiffSeconds > 0 ? valueDiff / timeDiffSeconds : 0.0;
    }

    // Statistics

    public MetricStatistics calculateStatistics(String metricName, Duration window) {
        Instant start = Instant.now().minus(window);
        List<Double> values = getDataPointsInRange(metricName, start, Instant.now()).stream()
                .map(MetricDataPoint::value)
                .sorted()
                .collect(Collectors.toList());
        
        if (values.isEmpty()) {
            return new MetricStatistics(metricName, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double avg = sum / values.size();
        double min = values.get(0);
        double max = values.get(values.size() - 1);
        
        // Calculate standard deviation
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - avg, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        double p50 = getPercentileValue(values, 50);
        double p95 = getPercentileValue(values, 95);
        double p99 = getPercentileValue(values, 99);
        
        return new MetricStatistics(metricName, values.size(), sum, avg, min, max, stdDev, p50, p95);
    }

    private double getPercentileValue(List<Double> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) return 0.0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    // Maintenance

    public void pruneOldDataPoints(Duration retention) {
        Instant threshold = Instant.now().minus(retention);
        log.info("Pruning data points older than: {}", threshold);
        
        metricDataPoints.values().forEach(list -> 
            list.removeIf(dp -> dp.timestamp().isBefore(threshold))
        );
    }

    public long getDataPointCount(String metricName) {
        return metricDataPoints.getOrDefault(metricName, Collections.emptyList()).size();
    }

    public long getTotalDataPointCount() {
        return metricDataPoints.values().stream()
                .mapToLong(List::size)
                .sum();
    }

    public void clear() {
        metricDefinitions.clear();
        metricDataPoints.clear();
    }

    // Statistics record
    public record MetricStatistics(
            String metricName,
            long count,
            double sum,
            double average,
            double min,
            double max,
            double stdDev,
            double p50,
            double p95
    ) {}
}
