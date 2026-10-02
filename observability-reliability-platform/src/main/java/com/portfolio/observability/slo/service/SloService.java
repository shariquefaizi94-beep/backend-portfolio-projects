package com.portfolio.observability.slo.service;

import com.portfolio.observability.metrics.service.MetricsService;
import com.portfolio.observability.slo.model.ServiceLevelObjective;
import com.portfolio.observability.slo.model.SloState;
import com.portfolio.observability.slo.model.SloStatus;
import com.portfolio.observability.slo.model.SloType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing Service Level Objectives (SLOs).
 * Provides SLO definition, tracking, error budget calculation, and compliance monitoring.
 */
@Service
public class SloService {

    private static final Logger log = LoggerFactory.getLogger(SloService.class);

    private final Map<UUID, ServiceLevelObjective> slos = new ConcurrentHashMap<>();
    private final Map<UUID, List<SloStatus>> statusHistory = new ConcurrentHashMap<>();
    private final MetricsService metricsService;

    public SloService(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    // SLO Management

    public ServiceLevelObjective createSlo(ServiceLevelObjective slo) {
        log.info("Creating SLO: {} for service: {} with target: {}%", 
                slo.name(), slo.service(), slo.target());
        slos.put(slo.id(), slo);
        return slo;
    }

    public ServiceLevelObjective createAvailabilitySlo(String name, String service, 
                                                        double target, Duration window) {
        ServiceLevelObjective slo = ServiceLevelObjective.availability(name, service, target, window);
        return createSlo(slo);
    }

    public ServiceLevelObjective createLatencySlo(String name, String service, 
                                                   double target, Duration window, Duration threshold) {
        ServiceLevelObjective slo = ServiceLevelObjective.latency(name, service, target, window, threshold);
        return createSlo(slo);
    }

    public Optional<ServiceLevelObjective> getSlo(UUID id) {
        return Optional.ofNullable(slos.get(id));
    }

    public Optional<ServiceLevelObjective> getSloByName(String name) {
        return slos.values().stream()
                .filter(s -> s.name().equals(name))
                .findFirst();
    }

    public List<ServiceLevelObjective> getAllSlos() {
        return new ArrayList<>(slos.values());
    }

    public List<ServiceLevelObjective> getSlosByService(String service) {
        return slos.values().stream()
                .filter(s -> s.service().equals(service))
                .collect(Collectors.toList());
    }

    public List<ServiceLevelObjective> getSlosByType(SloType type) {
        return slos.values().stream()
                .filter(s -> s.type() == type)
                .collect(Collectors.toList());
    }

    public List<ServiceLevelObjective> getEnabledSlos() {
        return slos.values().stream()
                .filter(ServiceLevelObjective::enabled)
                .collect(Collectors.toList());
    }

    public void deleteSlo(UUID id) {
        log.info("Deleting SLO: {}", id);
        slos.remove(id);
        statusHistory.remove(id);
    }

    // SLO Status Calculation

    public SloStatus calculateStatus(UUID sloId) {
        ServiceLevelObjective slo = slos.get(sloId);
        if (slo == null) {
            throw new IllegalArgumentException("SLO not found: " + sloId);
        }
        
        double currentSli = calculateSli(slo);
        SloStatus status = SloStatus.calculate(slo, currentSli);
        
        // Store in history
        statusHistory.computeIfAbsent(sloId, k -> 
            Collections.synchronizedList(new ArrayList<>())
        ).add(status);
        
        log.debug("SLO {} status: {} (SLI: {}, Error Budget: {}%)", 
                slo.name(), status.state(), currentSli, status.errorBudgetRemaining());
        
        return status;
    }

    public List<SloStatus> calculateAllStatuses() {
        return getEnabledSlos().stream()
                .map(slo -> calculateStatus(slo.id()))
                .collect(Collectors.toList());
    }

    private double calculateSli(ServiceLevelObjective slo) {
        // Calculate SLI based on SLO type
        String service = slo.service();
        Duration window = slo.window();
        
        switch (slo.type()) {
            case AVAILABILITY:
                return calculateAvailabilitySli(service, window);
            case LATENCY:
                return calculateLatencySli(service, window, slo);
            case THROUGHPUT:
                return calculateThroughputSli(service, window, slo);
            case ERROR_RATE:
                return calculateErrorRateSli(service, window);
            default:
                return 0.0;
        }
    }

    private double calculateAvailabilitySli(String service, Duration window) {
        // Calculate based on success rate metrics
        String totalMetric = service + "_requests_total";
        String errorMetric = service + "_requests_errors";
        
        double totalRequests = metricsService.calculateSum(totalMetric, window);
        double errorRequests = metricsService.calculateSum(errorMetric, window);
        
        if (totalRequests == 0) {
            return 100.0; // No requests = 100% availability
        }
        
        return ((totalRequests - errorRequests) / totalRequests) * 100.0;
    }

    private double calculateLatencySli(String service, Duration window, ServiceLevelObjective slo) {
        // Calculate percentage of requests under latency threshold
        String latencyMetric = service + "_request_latency_ms";
        String thresholdStr = slo.labels().get("threshold_ms");
        double threshold = thresholdStr != null ? Double.parseDouble(thresholdStr) : 200.0;
        
        // Get latency data points
        var dataPoints = metricsService.getDataPointsInRange(
                latencyMetric, 
                Instant.now().minus(window), 
                Instant.now()
        );
        
        if (dataPoints.isEmpty()) {
            return 100.0;
        }
        
        long underThreshold = dataPoints.stream()
                .filter(dp -> dp.value() <= threshold)
                .count();
        
        return ((double) underThreshold / dataPoints.size()) * 100.0;
    }

    private double calculateThroughputSli(String service, Duration window, ServiceLevelObjective slo) {
        String throughputMetric = service + "_throughput_rps";
        double currentThroughput = metricsService.calculateAverage(throughputMetric, window);
        
        // Compare against target (stored in labels)
        String targetStr = slo.labels().get("target_rps");
        double targetRps = targetStr != null ? Double.parseDouble(targetStr) : 1000.0;
        
        return Math.min(100.0, (currentThroughput / targetRps) * 100.0);
    }

    private double calculateErrorRateSli(String service, Duration window) {
        String errorRateMetric = service + "_error_rate";
        double errorRate = metricsService.calculateAverage(errorRateMetric, window);
        
        // Convert error rate to success rate
        return 100.0 - errorRate;
    }

    // Status Queries

    public Optional<SloStatus> getLatestStatus(UUID sloId) {
        return statusHistory.getOrDefault(sloId, Collections.emptyList()).stream()
                .max(Comparator.comparing(SloStatus::calculatedAt));
    }

    public List<SloStatus> getStatusHistory(UUID sloId, Instant start, Instant end) {
        return statusHistory.getOrDefault(sloId, Collections.emptyList()).stream()
                .filter(s -> !s.calculatedAt().isBefore(start) && !s.calculatedAt().isAfter(end))
                .sorted(Comparator.comparing(SloStatus::calculatedAt))
                .collect(Collectors.toList());
    }

    public List<SloStatus> getAllLatestStatuses() {
        return slos.keySet().stream()
                .map(this::getLatestStatus)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public List<SloStatus> getViolatedSlos() {
        return getAllLatestStatuses().stream()
                .filter(s -> s.state() == SloState.BREACHED)
                .collect(Collectors.toList());
    }

    public List<SloStatus> getAtRiskSlos() {
        return getAllLatestStatuses().stream()
                .filter(s -> s.state() == SloState.AT_RISK || s.state() == SloState.BREACHING)
                .collect(Collectors.toList());
    }

    public List<SloStatus> getHealthySlos() {
        return getAllLatestStatuses().stream()
                .filter(s -> s.state() == SloState.MET)
                .collect(Collectors.toList());
    }

    // Error Budget

    public ErrorBudgetSummary getErrorBudgetSummary(UUID sloId) {
        ServiceLevelObjective slo = slos.get(sloId);
        if (slo == null) {
            throw new IllegalArgumentException("SLO not found: " + sloId);
        }
        
        SloStatus status = getLatestStatus(sloId)
                .orElseGet(() -> calculateStatus(sloId));
        
        double totalBudgetMinutes = slo.errorBudgetDuration().toMinutes();
        double remainingMinutes = totalBudgetMinutes * (status.errorBudgetRemaining() / 100.0);
        double consumedMinutes = totalBudgetMinutes - remainingMinutes;
        
        // Calculate burn rate
        double burnRate = calculateBurnRate(sloId, Duration.ofHours(1));
        
        // Estimate time to exhaustion
        Duration timeToExhaustion = burnRate > 0 
                ? Duration.ofMinutes((long) (remainingMinutes / burnRate))
                : Duration.ofDays(365); // Effectively infinite
        
        return new ErrorBudgetSummary(
                slo.name(),
                slo.service(),
                slo.target(),
                status.currentValue(),
                totalBudgetMinutes,
                remainingMinutes,
                consumedMinutes,
                status.errorBudgetRemaining(),
                burnRate,
                timeToExhaustion,
                status.state()
        );
    }

    private double calculateBurnRate(UUID sloId, Duration window) {
        List<SloStatus> history = getStatusHistory(
                sloId, 
                Instant.now().minus(window), 
                Instant.now()
        );
        
        if (history.size() < 2) {
            return 0.0;
        }
        
        SloStatus oldest = history.get(0);
        SloStatus newest = history.get(history.size() - 1);
        
        double budgetConsumed = oldest.errorBudgetRemaining() - newest.errorBudgetRemaining();
        long minutesElapsed = Duration.between(oldest.calculatedAt(), newest.calculatedAt()).toMinutes();
        
        return minutesElapsed > 0 ? budgetConsumed / minutesElapsed : 0.0;
    }

    // Statistics

    public SloStatistics getStatistics() {
        List<SloStatus> statuses = getAllLatestStatuses();
        
        long total = slos.size();
        long enabled = getEnabledSlos().size();
        long met = statuses.stream().filter(s -> s.state() == SloState.MET).count();
        long atRisk = statuses.stream().filter(s -> s.state() == SloState.AT_RISK).count();
        long breaching = statuses.stream().filter(s -> s.state() == SloState.BREACHING).count();
        long breached = statuses.stream().filter(s -> s.state() == SloState.BREACHED).count();
        
        double overallCompliance = total > 0 ? (double) met / statuses.size() * 100.0 : 100.0;
        double avgErrorBudget = statuses.stream()
                .mapToDouble(SloStatus::errorBudgetRemaining)
                .average()
                .orElse(100.0);
        
        return new SloStatistics(total, enabled, met, atRisk, breaching, breached, 
                overallCompliance, avgErrorBudget);
    }

    // Maintenance

    public void pruneStatusHistory(Duration retention) {
        Instant threshold = Instant.now().minus(retention);
        statusHistory.values().forEach(list -> 
            list.removeIf(s -> s.calculatedAt().isBefore(threshold))
        );
    }

    public void clear() {
        slos.clear();
        statusHistory.clear();
    }

    // Inner types

    public record ErrorBudgetSummary(
            String sloName,
            String service,
            double target,
            double currentSli,
            double totalBudgetMinutes,
            double remainingMinutes,
            double consumedMinutes,
            double remainingPercentage,
            double burnRatePerMinute,
            Duration estimatedTimeToExhaustion,
            SloState state
    ) {}

    public record SloStatistics(
            long total,
            long enabled,
            long met,
            long atRisk,
            long breaching,
            long breached,
            double overallCompliance,
            double averageErrorBudgetRemaining
    ) {}
}
