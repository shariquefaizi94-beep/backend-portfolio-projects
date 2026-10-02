package com.portfolio.observability.alerting.repository;

import com.portfolio.observability.alerting.model.Alert;
import com.portfolio.observability.alerting.model.AlertRule;
import com.portfolio.observability.alerting.model.AlertSeverity;
import com.portfolio.observability.alerting.model.AlertStatus;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for alerts and alert rules.
 * Provides efficient querying for alert management and incident response.
 */
@Repository
public class AlertRepository {

    private final Map<UUID, Alert> alerts = new ConcurrentHashMap<>();
    private final Map<UUID, AlertRule> alertRules = new ConcurrentHashMap<>();

    // Alert operations

    public Alert save(Alert alert) {
        alerts.put(alert.id(), alert);
        return alert;
    }

    public Optional<Alert> findById(UUID id) {
        return Optional.ofNullable(alerts.get(id));
    }

    public List<Alert> findAll() {
        return new ArrayList<>(alerts.values());
    }

    public List<Alert> findByStatus(AlertStatus status) {
        return alerts.values().stream()
                .filter(a -> a.status() == status)
                .sorted(Comparator.comparing(Alert::startsAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Alert> findBySeverity(AlertSeverity severity) {
        return alerts.values().stream()
                .filter(a -> a.severity() == severity)
                .sorted(Comparator.comparing(Alert::startsAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Alert> findByLabel(String labelKey, String labelValue) {
        return alerts.values().stream()
                .filter(a -> a.labels() != null && labelValue.equals(a.labels().get(labelKey)))
                .sorted(Comparator.comparing(Alert::startsAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Alert> findActiveAlerts() {
        return alerts.values().stream()
                .filter(a -> a.status() == AlertStatus.FIRING || a.status() == AlertStatus.PENDING)
                .sorted(Comparator.comparing(Alert::severity)
                        .thenComparing(Alert::startsAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Alert> findByTimeRange(Instant start, Instant end) {
        return alerts.values().stream()
                .filter(a -> !a.startsAt().isBefore(start) && !a.startsAt().isAfter(end))
                .sorted(Comparator.comparing(Alert::startsAt).reversed())
                .collect(Collectors.toList());
    }

    public long countByStatus(AlertStatus status) {
        return alerts.values().stream()
                .filter(a -> a.status() == status)
                .count();
    }

    public long countBySeverity(AlertSeverity severity) {
        return alerts.values().stream()
                .filter(a -> a.severity() == severity)
                .count();
    }

    public void deleteById(UUID id) {
        alerts.remove(id);
    }

    public void deleteResolvedOlderThan(Instant threshold) {
        alerts.entrySet().removeIf(e -> 
            e.getValue().status() == AlertStatus.RESOLVED && 
            e.getValue().endsAt() != null &&
            e.getValue().endsAt().isBefore(threshold)
        );
    }

    // Alert Rule operations

    public AlertRule saveRule(AlertRule rule) {
        alertRules.put(rule.id(), rule);
        return rule;
    }

    public Optional<AlertRule> findRuleById(UUID id) {
        return Optional.ofNullable(alertRules.get(id));
    }

    public Optional<AlertRule> findRuleByName(String name) {
        return alertRules.values().stream()
                .filter(r -> name.equals(r.name()))
                .findFirst();
    }

    public List<AlertRule> findAllRules() {
        return new ArrayList<>(alertRules.values());
    }

    public List<AlertRule> findEnabledRules() {
        return alertRules.values().stream()
                .filter(AlertRule::enabled)
                .collect(Collectors.toList());
    }

    public List<AlertRule> findRulesBySeverity(AlertSeverity severity) {
        return alertRules.values().stream()
                .filter(r -> r.severity() == severity)
                .collect(Collectors.toList());
    }

    public void deleteRuleById(UUID id) {
        alertRules.remove(id);
    }

    public boolean existsRuleByName(String name) {
        return alertRules.values().stream()
                .anyMatch(r -> name.equals(r.name()));
    }

    public void clear() {
        alerts.clear();
        alertRules.clear();
    }
}
