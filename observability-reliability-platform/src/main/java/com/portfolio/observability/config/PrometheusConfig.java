package com.portfolio.observability.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Prometheus-specific metrics configuration.
 * Defines custom metrics exposed to Prometheus for platform monitoring.
 */
@Configuration
public class PrometheusConfig {

    private final AtomicInteger activeAlerts = new AtomicInteger(0);
    private final AtomicInteger activeSlos = new AtomicInteger(0);
    private final AtomicLong totalSpans = new AtomicLong(0);
    private final AtomicInteger healthyServices = new AtomicInteger(0);
    private final AtomicInteger unhealthyServices = new AtomicInteger(0);

    @Bean
    public Gauge activeAlertsGauge(MeterRegistry registry) {
        return Gauge.builder("observability_active_alerts", activeAlerts, AtomicInteger::get)
                .description("Number of currently active alerts")
                .tag("type", "alert")
                .register(registry);
    }

    @Bean
    public Gauge activeSlosGauge(MeterRegistry registry) {
        return Gauge.builder("observability_active_slos", activeSlos, AtomicInteger::get)
                .description("Number of active SLOs being monitored")
                .tag("type", "slo")
                .register(registry);
    }

    @Bean
    public Gauge totalSpansGauge(MeterRegistry registry) {
        return Gauge.builder("observability_total_spans", totalSpans, AtomicLong::get)
                .description("Total number of spans stored")
                .tag("type", "tracing")
                .register(registry);
    }

    @Bean
    public Gauge healthyServicesGauge(MeterRegistry registry) {
        return Gauge.builder("observability_healthy_services", healthyServices, AtomicInteger::get)
                .description("Number of healthy services")
                .tag("status", "healthy")
                .register(registry);
    }

    @Bean
    public Gauge unhealthyServicesGauge(MeterRegistry registry) {
        return Gauge.builder("observability_unhealthy_services", unhealthyServices, AtomicInteger::get)
                .description("Number of unhealthy services")
                .tag("status", "unhealthy")
                .register(registry);
    }

    @Bean
    public Counter alertsFiredCounter(MeterRegistry registry) {
        return Counter.builder("observability_alerts_fired_total")
                .description("Total number of alerts fired")
                .tag("type", "alert")
                .register(registry);
    }

    @Bean
    public Counter alertsResolvedCounter(MeterRegistry registry) {
        return Counter.builder("observability_alerts_resolved_total")
                .description("Total number of alerts resolved")
                .tag("type", "alert")
                .register(registry);
    }

    @Bean
    public Counter metricsRecordedCounter(MeterRegistry registry) {
        return Counter.builder("observability_metrics_recorded_total")
                .description("Total number of metric data points recorded")
                .tag("type", "metrics")
                .register(registry);
    }

    @Bean
    public Counter spansRecordedCounter(MeterRegistry registry) {
        return Counter.builder("observability_spans_recorded_total")
                .description("Total number of spans recorded")
                .tag("type", "tracing")
                .register(registry);
    }

    @Bean
    public Timer alertEvaluationTimer(MeterRegistry registry) {
        return Timer.builder("observability_alert_evaluation_duration")
                .description("Time taken to evaluate alert rules")
                .tag("type", "alert")
                .register(registry);
    }

    @Bean
    public Timer sloCalculationTimer(MeterRegistry registry) {
        return Timer.builder("observability_slo_calculation_duration")
                .description("Time taken to calculate SLO status")
                .tag("type", "slo")
                .register(registry);
    }

    @Bean
    public Timer healthCheckTimer(MeterRegistry registry) {
        return Timer.builder("observability_health_check_duration")
                .description("Time taken to perform health checks")
                .tag("type", "health")
                .register(registry);
    }

    // Accessor methods for updating gauge values
    public void setActiveAlerts(int count) {
        activeAlerts.set(count);
    }

    public void setActiveSlos(int count) {
        activeSlos.set(count);
    }

    public void setTotalSpans(long count) {
        totalSpans.set(count);
    }

    public void setHealthyServices(int count) {
        healthyServices.set(count);
    }

    public void setUnhealthyServices(int count) {
        unhealthyServices.set(count);
    }
}
