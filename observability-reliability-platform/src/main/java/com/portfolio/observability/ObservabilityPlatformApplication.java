package com.portfolio.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Observability and Reliability Platform - Main Application
 * 
 * A comprehensive observability platform demonstrating:
 * - Prometheus metrics collection and custom metrics
 * - Grafana dashboard configuration
 * - Distributed tracing with Zipkin
 * - Alerting rules and notification channels
 * - SLO/SLI definitions and monitoring
 * - Error budget tracking
 * - Health check aggregation
 * - SRE best practices implementation
 * 
 * @author Portfolio Project
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ObservabilityPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObservabilityPlatformApplication.class, args);
    }
}
