package com.portfolio.observability.metrics.controller;

import com.portfolio.observability.metrics.model.MetricDataPoint;
import com.portfolio.observability.metrics.model.MetricDefinition;
import com.portfolio.observability.metrics.model.MetricType;
import com.portfolio.observability.metrics.service.MetricsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST controller for metrics management.
 * Provides endpoints for metric registration, data recording, and querying.
 */
@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    // Metric Definition Endpoints

    @PostMapping("/definitions")
    public ResponseEntity<MetricDefinition> registerMetric(@RequestBody MetricDefinition definition) {
        MetricDefinition registered = metricsService.registerMetric(definition);
        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }

    @GetMapping("/definitions")
    public ResponseEntity<List<MetricDefinition>> getAllDefinitions() {
        return ResponseEntity.ok(metricsService.getAllMetricDefinitions());
    }

    @GetMapping("/definitions/{name}")
    public ResponseEntity<MetricDefinition> getDefinition(@PathVariable String name) {
        return metricsService.getMetricDefinition(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/definitions/type/{type}")
    public ResponseEntity<List<MetricDefinition>> getDefinitionsByType(@PathVariable MetricType type) {
        return ResponseEntity.ok(metricsService.getMetricsByType(type));
    }

    @DeleteMapping("/definitions/{name}")
    public ResponseEntity<Void> unregisterMetric(@PathVariable String name) {
        metricsService.unregisterMetric(name);
        return ResponseEntity.noContent().build();
    }

    // Data Point Endpoints

    @PostMapping("/data")
    public ResponseEntity<MetricDataPoint> recordDataPoint(@RequestBody MetricDataPoint dataPoint) {
        MetricDataPoint recorded = metricsService.recordDataPoint(dataPoint);
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }

    @PostMapping("/data/batch")
    public ResponseEntity<List<MetricDataPoint>> recordDataPoints(@RequestBody List<MetricDataPoint> dataPoints) {
        List<MetricDataPoint> recorded = metricsService.recordDataPoints(dataPoints);
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }

    @PostMapping("/data/{metricName}")
    public ResponseEntity<MetricDataPoint> recordValue(
            @PathVariable String metricName,
            @RequestParam double value,
            @RequestParam(required = false) Map<String, String> labels) {
        MetricDataPoint recorded = labels != null && !labels.isEmpty()
                ? metricsService.recordValue(metricName, value, labels)
                : metricsService.recordValue(metricName, value);
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }

    @GetMapping("/data/{metricName}")
    public ResponseEntity<List<MetricDataPoint>> getDataPoints(@PathVariable String metricName) {
        return ResponseEntity.ok(metricsService.getDataPoints(metricName));
    }

    @GetMapping("/data/{metricName}/range")
    public ResponseEntity<List<MetricDataPoint>> getDataPointsInRange(
            @PathVariable String metricName,
            @RequestParam Instant start,
            @RequestParam Instant end) {
        return ResponseEntity.ok(metricsService.getDataPointsInRange(metricName, start, end));
    }

    @GetMapping("/data/{metricName}/latest")
    public ResponseEntity<MetricDataPoint> getLatestDataPoint(@PathVariable String metricName) {
        return metricsService.getLatestDataPoint(metricName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/data/latest")
    public ResponseEntity<Map<String, Double>> getLatestValues() {
        return ResponseEntity.ok(metricsService.getLatestValues());
    }

    // Aggregation Endpoints

    @GetMapping("/data/{metricName}/avg")
    public ResponseEntity<Double> getAverage(
            @PathVariable String metricName,
            @RequestParam(defaultValue = "PT1H") Duration window) {
        return ResponseEntity.ok(metricsService.calculateAverage(metricName, window));
    }

    @GetMapping("/data/{metricName}/sum")
    public ResponseEntity<Double> getSum(
            @PathVariable String metricName,
            @RequestParam(defaultValue = "PT1H") Duration window) {
        return ResponseEntity.ok(metricsService.calculateSum(metricName, window));
    }

    @GetMapping("/data/{metricName}/min")
    public ResponseEntity<Double> getMin(
            @PathVariable String metricName,
            @RequestParam(defaultValue = "PT1H") Duration window) {
        return ResponseEntity.ok(metricsService.calculateMin(metricName, window));
    }

    @GetMapping("/data/{metricName}/max")
    public ResponseEntity<Double> getMax(
            @PathVariable String metricName,
            @RequestParam(defaultValue = "PT1H") Duration window) {
        return ResponseEntity.ok(metricsService.calculateMax(metricName, window));
    }

    @GetMapping("/data/{metricName}/percentile")
    public ResponseEntity<Double> getPercentile(
            @PathVariable String metricName,
            @RequestParam double percentile,
            @RequestParam(defaultValue = "PT1H") Duration window) {
        return ResponseEntity.ok(metricsService.calculatePercentile(metricName, window, percentile));
    }

    @GetMapping("/data/{metricName}/rate")
    public ResponseEntity<Double> getRate(
            @PathVariable String metricName,
            @RequestParam(defaultValue = "PT1H") Duration window) {
        return ResponseEntity.ok(metricsService.calculateRate(metricName, window));
    }

    @GetMapping("/data/{metricName}/statistics")
    public ResponseEntity<MetricsService.MetricStatistics> getStatistics(
            @PathVariable String metricName,
            @RequestParam(defaultValue = "PT1H") Duration window) {
        return ResponseEntity.ok(metricsService.calculateStatistics(metricName, window));
    }

    // Counter Endpoints

    @PostMapping("/counters/{metricName}/increment")
    public ResponseEntity<Void> incrementCounter(
            @PathVariable String metricName,
            @RequestParam(defaultValue = "1.0") double amount,
            @RequestParam(required = false) Map<String, String> labels) {
        metricsService.incrementCounter(metricName, amount, labels != null ? labels : Map.of());
        return ResponseEntity.ok().build();
    }

    // Maintenance Endpoints

    @DeleteMapping("/data/prune")
    public ResponseEntity<Void> pruneOldDataPoints(@RequestParam Duration retention) {
        metricsService.pruneOldDataPoints(retention);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/data/{metricName}/count")
    public ResponseEntity<Long> getDataPointCount(@PathVariable String metricName) {
        return ResponseEntity.ok(metricsService.getDataPointCount(metricName));
    }

    @GetMapping("/data/count")
    public ResponseEntity<Long> getTotalDataPointCount() {
        return ResponseEntity.ok(metricsService.getTotalDataPointCount());
    }
}
