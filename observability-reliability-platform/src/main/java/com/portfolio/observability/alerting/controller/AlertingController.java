package com.portfolio.observability.alerting.controller;

import com.portfolio.observability.alerting.model.Alert;
import com.portfolio.observability.alerting.model.AlertRule;
import com.portfolio.observability.alerting.model.AlertSeverity;
import com.portfolio.observability.alerting.model.AlertStatus;
import com.portfolio.observability.alerting.service.AlertingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for alerting management.
 * Provides endpoints for alert rules and alert lifecycle management.
 */
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertingController {

    private final AlertingService alertingService;

    public AlertingController(AlertingService alertingService) {
        this.alertingService = alertingService;
    }

    // Alert Rule Endpoints

    @PostMapping("/rules")
    public ResponseEntity<AlertRule> createRule(@RequestBody AlertRule rule) {
        AlertRule created = alertingService.createRule(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/rules")
    public ResponseEntity<List<AlertRule>> getAllRules() {
        return ResponseEntity.ok(alertingService.getAllRules());
    }

    @GetMapping("/rules/enabled")
    public ResponseEntity<List<AlertRule>> getEnabledRules() {
        return ResponseEntity.ok(alertingService.getEnabledRules());
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<AlertRule> getRule(@PathVariable UUID id) {
        return alertingService.getRule(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rules/name/{name}")
    public ResponseEntity<AlertRule> getRuleByName(@PathVariable String name) {
        return alertingService.getRuleByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<AlertRule> updateRule(@PathVariable UUID id, @RequestBody AlertRule rule) {
        try {
            AlertRule updated = alertingService.updateRule(id, rule);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/rules/{id}/enable")
    public ResponseEntity<AlertRule> enableRule(@PathVariable UUID id) {
        try {
            AlertRule enabled = alertingService.enableRule(id);
            return ResponseEntity.ok(enabled);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/rules/{id}/disable")
    public ResponseEntity<AlertRule> disableRule(@PathVariable UUID id) {
        try {
            AlertRule disabled = alertingService.disableRule(id);
            return ResponseEntity.ok(disabled);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        alertingService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    // Alert Endpoints

    @PostMapping
    public ResponseEntity<Alert> createAlert(@RequestBody CreateAlertRequest request) {
        Alert created = alertingService.createAlert(
                request.name(),
                request.description(),
                request.severity(),
                request.labels(),
                request.annotations()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Alert>> getAllAlerts() {
        return ResponseEntity.ok(alertingService.getAllAlerts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alert> getAlert(@PathVariable UUID id) {
        return alertingService.getAlert(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Alert>> getActiveAlerts() {
        return ResponseEntity.ok(alertingService.getActiveAlerts());
    }

    @GetMapping("/firing")
    public ResponseEntity<List<Alert>> getFiringAlerts() {
        return ResponseEntity.ok(alertingService.getFiringAlerts());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Alert>> getAlertsByStatus(@PathVariable AlertStatus status) {
        return ResponseEntity.ok(alertingService.getAlertsByStatus(status));
    }

    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<Alert>> getAlertsBySeverity(@PathVariable AlertSeverity severity) {
        return ResponseEntity.ok(alertingService.getAlertsBySeverity(severity));
    }

    // Alert State Transitions

    @PostMapping("/{id}/fire")
    public ResponseEntity<Alert> fireAlert(@PathVariable UUID id) {
        try {
            Alert fired = alertingService.fireAlert(id);
            return ResponseEntity.ok(fired);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Alert> resolveAlert(@PathVariable UUID id) {
        try {
            Alert resolved = alertingService.resolveAlert(id);
            return ResponseEntity.ok(resolved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Alert> acknowledgeAlert(@PathVariable UUID id) {
        try {
            Alert acknowledged = alertingService.acknowledgeAlert(id);
            return ResponseEntity.ok(acknowledged);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/silence")
    public ResponseEntity<Alert> silenceAlert(@PathVariable UUID id) {
        try {
            Alert silenced = alertingService.silenceAlert(id);
            return ResponseEntity.ok(silenced);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable UUID id) {
        alertingService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }

    // Rule Evaluation

    @PostMapping("/evaluate")
    public ResponseEntity<List<Alert>> evaluateRules() {
        List<Alert> triggered = alertingService.evaluateRules();
        return ResponseEntity.ok(triggered);
    }

    // Statistics

    @GetMapping("/statistics")
    public ResponseEntity<AlertingService.AlertStatistics> getStatistics() {
        return ResponseEntity.ok(alertingService.getStatistics());
    }

    // Maintenance

    @DeleteMapping("/prune")
    public ResponseEntity<Void> pruneResolvedAlerts(@RequestParam Duration retention) {
        alertingService.pruneResolvedAlerts(retention);
        return ResponseEntity.noContent().build();
    }

    // Request DTOs

    public record CreateAlertRequest(
            String name,
            String description,
            AlertSeverity severity,
            Map<String, String> labels,
            Map<String, String> annotations
    ) {}
}
