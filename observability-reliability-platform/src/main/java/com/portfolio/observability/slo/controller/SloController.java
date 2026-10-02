package com.portfolio.observability.slo.controller;

import com.portfolio.observability.slo.model.ServiceLevelObjective;
import com.portfolio.observability.slo.model.SloStatus;
import com.portfolio.observability.slo.model.SloType;
import com.portfolio.observability.slo.service.SloService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Service Level Objective (SLO) management.
 * Provides endpoints for SLO definition, status tracking, and error budget monitoring.
 */
@RestController
@RequestMapping("/api/v1/slos")
public class SloController {

    private final SloService sloService;

    public SloController(SloService sloService) {
        this.sloService = sloService;
    }

    // SLO Definition Endpoints

    @PostMapping
    public ResponseEntity<ServiceLevelObjective> createSlo(@RequestBody ServiceLevelObjective slo) {
        ServiceLevelObjective created = sloService.createSlo(slo);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/availability")
    public ResponseEntity<ServiceLevelObjective> createAvailabilitySlo(
            @RequestBody CreateAvailabilitySloRequest request) {
        ServiceLevelObjective slo = sloService.createAvailabilitySlo(
                request.name(), request.service(), request.target(), request.window());
        return ResponseEntity.status(HttpStatus.CREATED).body(slo);
    }

    @PostMapping("/latency")
    public ResponseEntity<ServiceLevelObjective> createLatencySlo(
            @RequestBody CreateLatencySloRequest request) {
        ServiceLevelObjective slo = sloService.createLatencySlo(
                request.name(), request.service(), request.target(), 
                request.window(), request.threshold());
        return ResponseEntity.status(HttpStatus.CREATED).body(slo);
    }

    @GetMapping
    public ResponseEntity<List<ServiceLevelObjective>> getAllSlos() {
        return ResponseEntity.ok(sloService.getAllSlos());
    }

    @GetMapping("/enabled")
    public ResponseEntity<List<ServiceLevelObjective>> getEnabledSlos() {
        return ResponseEntity.ok(sloService.getEnabledSlos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceLevelObjective> getSlo(@PathVariable UUID id) {
        return sloService.getSlo(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<ServiceLevelObjective> getSloByName(@PathVariable String name) {
        return sloService.getSloByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/service/{service}")
    public ResponseEntity<List<ServiceLevelObjective>> getSlosByService(@PathVariable String service) {
        return ResponseEntity.ok(sloService.getSlosByService(service));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<ServiceLevelObjective>> getSlosByType(@PathVariable SloType type) {
        return ResponseEntity.ok(sloService.getSlosByType(type));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlo(@PathVariable UUID id) {
        sloService.deleteSlo(id);
        return ResponseEntity.noContent().build();
    }

    // SLO Status Endpoints

    @GetMapping("/{id}/status")
    public ResponseEntity<SloStatus> calculateStatus(@PathVariable UUID id) {
        try {
            SloStatus status = sloService.calculateStatus(id);
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/status")
    public ResponseEntity<List<SloStatus>> calculateAllStatuses() {
        return ResponseEntity.ok(sloService.calculateAllStatuses());
    }

    @GetMapping("/{id}/status/latest")
    public ResponseEntity<SloStatus> getLatestStatus(@PathVariable UUID id) {
        return sloService.getLatestStatus(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/status/history")
    public ResponseEntity<List<SloStatus>> getStatusHistory(
            @PathVariable UUID id,
            @RequestParam Instant start,
            @RequestParam Instant end) {
        return ResponseEntity.ok(sloService.getStatusHistory(id, start, end));
    }

    @GetMapping("/status/latest")
    public ResponseEntity<List<SloStatus>> getAllLatestStatuses() {
        return ResponseEntity.ok(sloService.getAllLatestStatuses());
    }

    @GetMapping("/status/violated")
    public ResponseEntity<List<SloStatus>> getViolatedSlos() {
        return ResponseEntity.ok(sloService.getViolatedSlos());
    }

    @GetMapping("/status/at-risk")
    public ResponseEntity<List<SloStatus>> getAtRiskSlos() {
        return ResponseEntity.ok(sloService.getAtRiskSlos());
    }

    @GetMapping("/status/healthy")
    public ResponseEntity<List<SloStatus>> getHealthySlos() {
        return ResponseEntity.ok(sloService.getHealthySlos());
    }

    // Error Budget Endpoints

    @GetMapping("/{id}/error-budget")
    public ResponseEntity<SloService.ErrorBudgetSummary> getErrorBudgetSummary(@PathVariable UUID id) {
        try {
            SloService.ErrorBudgetSummary summary = sloService.getErrorBudgetSummary(id);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Statistics

    @GetMapping("/statistics")
    public ResponseEntity<SloService.SloStatistics> getStatistics() {
        return ResponseEntity.ok(sloService.getStatistics());
    }

    // Maintenance

    @DeleteMapping("/status/prune")
    public ResponseEntity<Void> pruneStatusHistory(@RequestParam Duration retention) {
        sloService.pruneStatusHistory(retention);
        return ResponseEntity.noContent().build();
    }

    // Request DTOs

    public record CreateAvailabilitySloRequest(
            String name,
            String service,
            double target,
            Duration window
    ) {}

    public record CreateLatencySloRequest(
            String name,
            String service,
            double target,
            Duration window,
            Duration threshold
    ) {}
}
