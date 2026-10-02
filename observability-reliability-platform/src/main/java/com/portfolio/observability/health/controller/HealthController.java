package com.portfolio.observability.health.controller;

import com.portfolio.observability.health.model.ComponentHealth;
import com.portfolio.observability.health.model.HealthStatus;
import com.portfolio.observability.health.model.ServiceHealth;
import com.portfolio.observability.health.service.HealthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST controller for health monitoring.
 * Provides endpoints for service health checks, status tracking, and health reporting.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    // Health Check Endpoints

    @PostMapping("/check/{serviceName}")
    public ResponseEntity<ServiceHealth> checkService(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "1.0.0") String version) {
        ServiceHealth health = healthService.checkService(serviceName, version);
        HttpStatus status = health.isHealthy() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(health);
    }

    @PostMapping
    public ResponseEntity<ServiceHealth> recordHealth(@RequestBody ServiceHealth health) {
        ServiceHealth recorded = healthService.recordHealth(health);
        HttpStatus status = recorded.isHealthy() ? HttpStatus.CREATED : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(recorded);
    }

    // Service Health Queries

    @GetMapping("/{serviceName}")
    public ResponseEntity<ServiceHealth> getServiceHealth(@PathVariable String serviceName) {
        return healthService.getServiceHealth(serviceName)
                .map(health -> {
                    HttpStatus status = health.isHealthy() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
                    return ResponseEntity.status(status).body(health);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ServiceHealth>> getAllServiceHealth() {
        return ResponseEntity.ok(healthService.getAllServiceHealth());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ServiceHealth>> getServicesByStatus(@PathVariable HealthStatus status) {
        return ResponseEntity.ok(healthService.getServicesByStatus(status));
    }

    @GetMapping("/unhealthy")
    public ResponseEntity<List<ServiceHealth>> getUnhealthyServices() {
        List<ServiceHealth> unhealthy = healthService.getUnhealthyServices();
        HttpStatus status = unhealthy.isEmpty() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(unhealthy);
    }

    @GetMapping("/degraded")
    public ResponseEntity<List<ServiceHealth>> getDegradedServices() {
        return ResponseEntity.ok(healthService.getDegradedServices());
    }

    @GetMapping("/down")
    public ResponseEntity<List<ServiceHealth>> getDownServices() {
        List<ServiceHealth> down = healthService.getDownServices();
        HttpStatus status = down.isEmpty() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(down);
    }

    @GetMapping("/status-map")
    public ResponseEntity<Map<String, HealthStatus>> getServiceStatusMap() {
        return ResponseEntity.ok(healthService.getServiceStatusMap());
    }

    // History Endpoints

    @GetMapping("/{serviceName}/history")
    public ResponseEntity<List<ServiceHealth>> getHealthHistory(
            @PathVariable String serviceName,
            @RequestParam Instant start,
            @RequestParam Instant end) {
        return ResponseEntity.ok(healthService.getHealthHistory(serviceName, start, end));
    }

    @GetMapping("/{serviceName}/uptime")
    public ResponseEntity<Double> getUptimePercentage(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "P7D") Duration window) {
        return ResponseEntity.ok(healthService.calculateUptimePercentage(serviceName, window));
    }

    @GetMapping("/{serviceName}/downtime")
    public ResponseEntity<Duration> getDowntime(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "P7D") Duration window) {
        return ResponseEntity.ok(healthService.calculateDowntime(serviceName, window));
    }

    // Component Health Endpoints

    @GetMapping("/components/unhealthy")
    public ResponseEntity<List<ComponentHealth>> getUnhealthyComponents() {
        return ResponseEntity.ok(healthService.getUnhealthyComponents());
    }

    @GetMapping("/components")
    public ResponseEntity<Map<String, List<ComponentHealth>>> getComponentHealthByService() {
        return ResponseEntity.ok(healthService.getComponentHealthByService());
    }

    // Overall Health Summary

    @GetMapping("/summary")
    public ResponseEntity<HealthService.OverallHealthSummary> getOverallHealthSummary() {
        HealthService.OverallHealthSummary summary = healthService.getOverallHealthSummary();
        HttpStatus status = summary.status() == HealthStatus.UP ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(summary);
    }

    @GetMapping("/distribution")
    public ResponseEntity<Map<HealthStatus, Long>> getStatusDistribution() {
        return ResponseEntity.ok(healthService.getStatusDistribution());
    }

    // Statistics

    @GetMapping("/statistics")
    public ResponseEntity<HealthService.HealthStatistics> getStatistics() {
        return ResponseEntity.ok(healthService.getStatistics());
    }

    // Liveness and Readiness Probes (Kubernetes-style)

    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> livenessProbe() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readinessProbe() {
        HealthService.OverallHealthSummary summary = healthService.getOverallHealthSummary();
        
        boolean ready = summary.status() == HealthStatus.UP || summary.status() == HealthStatus.DEGRADED;
        
        Map<String, Object> response = Map.of(
                "status", ready ? "UP" : "DOWN",
                "services", Map.of(
                        "up", summary.servicesUp(),
                        "degraded", summary.servicesDegraded(),
                        "down", summary.servicesDown()
                )
        );
        
        return ready 
                ? ResponseEntity.ok(response) 
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    // Health Check Registration

    @GetMapping("/checks")
    public ResponseEntity<List<String>> getRegisteredHealthChecks() {
        return ResponseEntity.ok(healthService.getRegisteredHealthChecks());
    }

    // Maintenance

    @DeleteMapping("/{serviceName}")
    public ResponseEntity<Void> removeService(@PathVariable String serviceName) {
        healthService.removeService(serviceName);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/history/prune")
    public ResponseEntity<Void> pruneHealthHistory(@RequestParam Duration retention) {
        healthService.pruneHealthHistory(retention);
        return ResponseEntity.noContent().build();
    }
}
