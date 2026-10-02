package com.portfolio.observability.config;

import com.portfolio.observability.alerting.service.AlertingService;
import com.portfolio.observability.health.service.HealthService;
import com.portfolio.observability.metrics.service.MetricsService;
import com.portfolio.observability.slo.service.SloService;
import com.portfolio.observability.tracing.service.TracingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

/**
 * Scheduling configuration for periodic observability tasks.
 * Handles alert evaluation, SLO calculation, and data retention.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);

    private final AlertingService alertingService;
    private final SloService sloService;
    private final MetricsService metricsService;
    private final TracingService tracingService;
    private final HealthService healthService;
    private final PrometheusConfig prometheusConfig;

    @Value("${observability.retention.metrics:P7D}")
    private Duration metricsRetention;

    @Value("${observability.retention.traces:P1D}")
    private Duration tracesRetention;

    @Value("${observability.retention.alerts:P30D}")
    private Duration alertsRetention;

    @Value("${observability.retention.health:P7D}")
    private Duration healthRetention;

    @Value("${observability.retention.slo-status:P30D}")
    private Duration sloStatusRetention;

    public SchedulingConfig(AlertingService alertingService, SloService sloService,
                           MetricsService metricsService, TracingService tracingService,
                           HealthService healthService, PrometheusConfig prometheusConfig) {
        this.alertingService = alertingService;
        this.sloService = sloService;
        this.metricsService = metricsService;
        this.tracingService = tracingService;
        this.healthService = healthService;
        this.prometheusConfig = prometheusConfig;
    }

    /**
     * Evaluate alert rules every 30 seconds.
     */
    @Scheduled(fixedRateString = "${observability.alert.evaluation-interval:30000}")
    public void evaluateAlertRules() {
        log.debug("Evaluating alert rules...");
        try {
            var triggered = alertingService.evaluateRules();
            if (!triggered.isEmpty()) {
                log.info("Triggered {} alerts", triggered.size());
            }
            updatePrometheusMetrics();
        } catch (Exception e) {
            log.error("Error evaluating alert rules: {}", e.getMessage());
        }
    }

    /**
     * Calculate SLO status every minute.
     */
    @Scheduled(fixedRateString = "${observability.slo.calculation-interval:60000}")
    public void calculateSloStatus() {
        log.debug("Calculating SLO status...");
        try {
            var statuses = sloService.calculateAllStatuses();
            var atRisk = sloService.getAtRiskSlos();
            if (!atRisk.isEmpty()) {
                log.warn("{} SLOs are at risk or violated", atRisk.size());
            }
        } catch (Exception e) {
            log.error("Error calculating SLO status: {}", e.getMessage());
        }
    }

    /**
     * Prune old data every hour.
     */
    @Scheduled(fixedRateString = "${observability.retention.prune-interval:3600000}")
    public void pruneOldData() {
        log.info("Pruning old observability data...");
        try {
            metricsService.pruneOldDataPoints(metricsRetention);
            tracingService.pruneOldSpans(tracesRetention);
            alertingService.pruneResolvedAlerts(alertsRetention);
            healthService.pruneHealthHistory(healthRetention);
            sloService.pruneStatusHistory(sloStatusRetention);
            log.info("Data pruning completed");
        } catch (Exception e) {
            log.error("Error pruning old data: {}", e.getMessage());
        }
    }

    /**
     * Update Prometheus metrics gauges every 15 seconds.
     */
    @Scheduled(fixedRate = 15000)
    public void updatePrometheusMetrics() {
        try {
            var alertStats = alertingService.getStatistics();
            prometheusConfig.setActiveAlerts((int) alertStats.firing());

            var sloStats = sloService.getStatistics();
            prometheusConfig.setActiveSlos((int) sloStats.enabled());

            var healthStats = healthService.getStatistics();
            prometheusConfig.setHealthyServices((int) healthStats.servicesUp());
            prometheusConfig.setUnhealthyServices(
                    (int) (healthStats.servicesDegraded() + healthStats.servicesDown()));

            var tracingStats = tracingService.getStatistics();
            prometheusConfig.setTotalSpans(tracingStats.totalSpans());
        } catch (Exception e) {
            log.debug("Error updating Prometheus metrics: {}", e.getMessage());
        }
    }
}
