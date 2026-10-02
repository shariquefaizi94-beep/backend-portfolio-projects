package com.portfolio.observability.alerting.service;

import com.portfolio.observability.alerting.model.Alert;
import com.portfolio.observability.alerting.model.AlertRule;
import com.portfolio.observability.alerting.model.AlertSeverity;
import com.portfolio.observability.alerting.model.AlertStatus;
import com.portfolio.observability.metrics.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing alerts and alert rules.
 * Provides comprehensive alerting capabilities including rule evaluation,
 * alert lifecycle management, and notification handling.
 */
@Service
public class AlertingService {

    private static final Logger log = LoggerFactory.getLogger(AlertingService.class);

    private final Map<UUID, Alert> alerts = new ConcurrentHashMap<>();
    private final Map<UUID, AlertRule> alertRules = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> pendingAlertStartTimes = new ConcurrentHashMap<>();
    private final MetricsService metricsService;
    private final List<AlertNotificationHandler> notificationHandlers = new ArrayList<>();

    public AlertingService(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    // Alert Rule Management

    public AlertRule createRule(AlertRule rule) {
        log.info("Creating alert rule: {} with severity: {}", rule.name(), rule.severity());
        alertRules.put(rule.id(), rule);
        return rule;
    }

    public Optional<AlertRule> getRule(UUID id) {
        return Optional.ofNullable(alertRules.get(id));
    }

    public Optional<AlertRule> getRuleByName(String name) {
        return alertRules.values().stream()
                .filter(r -> r.name().equals(name))
                .findFirst();
    }

    public List<AlertRule> getAllRules() {
        return new ArrayList<>(alertRules.values());
    }

    public List<AlertRule> getEnabledRules() {
        return alertRules.values().stream()
                .filter(AlertRule::enabled)
                .collect(Collectors.toList());
    }

    public AlertRule updateRule(UUID id, AlertRule updatedRule) {
        if (!alertRules.containsKey(id)) {
            throw new IllegalArgumentException("Alert rule not found: " + id);
        }
        alertRules.put(id, updatedRule);
        return updatedRule;
    }

    public AlertRule enableRule(UUID id) {
        AlertRule rule = alertRules.get(id);
        if (rule == null) {
            throw new IllegalArgumentException("Alert rule not found: " + id);
        }
        AlertRule enabled = rule.enable();
        alertRules.put(id, enabled);
        return enabled;
    }

    public AlertRule disableRule(UUID id) {
        AlertRule rule = alertRules.get(id);
        if (rule == null) {
            throw new IllegalArgumentException("Alert rule not found: " + id);
        }
        AlertRule disabled = rule.disable();
        alertRules.put(id, disabled);
        return disabled;
    }

    public void deleteRule(UUID id) {
        log.info("Deleting alert rule: {}", id);
        alertRules.remove(id);
    }

    // Alert Management

    public Alert createAlert(String name, String description, AlertSeverity severity,
                             Map<String, String> labels, Map<String, String> annotations) {
        Alert alert = Alert.create(name, description, severity, labels, annotations);
        alerts.put(alert.id(), alert);
        log.info("Created alert: {} with severity: {}", name, severity);
        return alert;
    }

    public Alert createAlertFromRule(AlertRule rule, Map<String, String> additionalLabels) {
        Map<String, String> labels = new HashMap<>(rule.labels());
        labels.putAll(additionalLabels);
        
        return createAlert(
                rule.name(),
                rule.description(),
                rule.severity(),
                labels,
                rule.annotations()
        );
    }

    public Optional<Alert> getAlert(UUID id) {
        return Optional.ofNullable(alerts.get(id));
    }

    public List<Alert> getAllAlerts() {
        return new ArrayList<>(alerts.values());
    }

    public List<Alert> getAlertsByStatus(AlertStatus status) {
        return alerts.values().stream()
                .filter(a -> a.status() == status)
                .sorted(Comparator.comparing(Alert::startsAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Alert> getAlertsBySeverity(AlertSeverity severity) {
        return alerts.values().stream()
                .filter(a -> a.severity() == severity)
                .sorted(Comparator.comparing(Alert::startsAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Alert> getActiveAlerts() {
        return alerts.values().stream()
                .filter(a -> a.status() == AlertStatus.FIRING || a.status() == AlertStatus.PENDING)
                .sorted(Comparator.comparing(Alert::severity)
                        .thenComparing(Alert::startsAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Alert> getFiringAlerts() {
        return getAlertsByStatus(AlertStatus.FIRING);
    }

    public Alert fireAlert(UUID alertId) {
        Alert alert = alerts.get(alertId);
        if (alert == null) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }
        Alert fired = alert.fire();
        alerts.put(alertId, fired);
        notifyHandlers(fired, AlertNotificationType.FIRING);
        log.warn("Alert FIRING: {} - {}", fired.name(), fired.description());
        return fired;
    }

    public Alert resolveAlert(UUID alertId) {
        Alert alert = alerts.get(alertId);
        if (alert == null) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }
        Alert resolved = alert.resolve();
        alerts.put(alertId, resolved);
        notifyHandlers(resolved, AlertNotificationType.RESOLVED);
        log.info("Alert RESOLVED: {}", resolved.name());
        return resolved;
    }

    public Alert acknowledgeAlert(UUID alertId) {
        Alert alert = alerts.get(alertId);
        if (alert == null) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }
        Alert acknowledged = alert.acknowledge();
        alerts.put(alertId, acknowledged);
        notifyHandlers(acknowledged, AlertNotificationType.ACKNOWLEDGED);
        log.info("Alert ACKNOWLEDGED: {}", acknowledged.name());
        return acknowledged;
    }

    public Alert silenceAlert(UUID alertId) {
        Alert alert = alerts.get(alertId);
        if (alert == null) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }
        Alert silenced = alert.silence();
        alerts.put(alertId, silenced);
        log.info("Alert SILENCED: {}", silenced.name());
        return silenced;
    }

    public void deleteAlert(UUID id) {
        alerts.remove(id);
        pendingAlertStartTimes.remove(id);
    }

    // Rule Evaluation

    public List<Alert> evaluateRules() {
        List<Alert> triggeredAlerts = new ArrayList<>();
        
        for (AlertRule rule : getEnabledRules()) {
            try {
                boolean conditionMet = evaluateRuleCondition(rule);
                
                if (conditionMet) {
                    Alert alert = handleConditionMet(rule);
                    if (alert != null && alert.status() == AlertStatus.FIRING) {
                        triggeredAlerts.add(alert);
                    }
                } else {
                    handleConditionNotMet(rule);
                }
            } catch (Exception e) {
                log.error("Error evaluating rule: {} - {}", rule.name(), e.getMessage());
            }
        }
        
        return triggeredAlerts;
    }

    private boolean evaluateRuleCondition(AlertRule rule) {
        // Simplified expression evaluation
        // In production, this would use a proper expression parser (e.g., PromQL)
        String expression = rule.expression();
        
        // Parse simple threshold expressions like "metric_name > 100"
        if (expression.contains(">")) {
            String[] parts = expression.split(">");
            String metricName = parts[0].trim();
            double threshold = Double.parseDouble(parts[1].trim());
            
            return metricsService.getLatestDataPoint(metricName)
                    .map(dp -> dp.value() > threshold)
                    .orElse(false);
        } else if (expression.contains("<")) {
            String[] parts = expression.split("<");
            String metricName = parts[0].trim();
            double threshold = Double.parseDouble(parts[1].trim());
            
            return metricsService.getLatestDataPoint(metricName)
                    .map(dp -> dp.value() < threshold)
                    .orElse(false);
        }
        
        return false;
    }

    private Alert handleConditionMet(AlertRule rule) {
        // Check if there's already an alert for this rule
        Optional<Alert> existingAlert = findAlertForRule(rule.id());
        
        if (existingAlert.isPresent()) {
            Alert alert = existingAlert.get();
            if (alert.status() == AlertStatus.PENDING) {
                // Check if for-duration has elapsed
                Instant pendingStart = pendingAlertStartTimes.get(alert.id());
                if (pendingStart != null && 
                    Duration.between(pendingStart, Instant.now()).compareTo(rule.forDuration()) >= 0) {
                    return fireAlert(alert.id());
                }
            }
            return alert;
        } else {
            // Create new pending alert
            Alert alert = createAlertFromRule(rule, Map.of("rule_id", rule.id().toString()));
            pendingAlertStartTimes.put(alert.id(), Instant.now());
            
            // If no for-duration, fire immediately
            if (rule.forDuration().isZero()) {
                return fireAlert(alert.id());
            }
            
            return alert;
        }
    }

    private void handleConditionNotMet(AlertRule rule) {
        findAlertForRule(rule.id()).ifPresent(alert -> {
            if (alert.status() == AlertStatus.FIRING || alert.status() == AlertStatus.PENDING) {
                resolveAlert(alert.id());
            }
            pendingAlertStartTimes.remove(alert.id());
        });
    }

    private Optional<Alert> findAlertForRule(UUID ruleId) {
        return alerts.values().stream()
                .filter(a -> ruleId.toString().equals(a.labels().get("rule_id")))
                .filter(a -> a.status() != AlertStatus.RESOLVED)
                .findFirst();
    }

    // Notification Handling

    public void registerNotificationHandler(AlertNotificationHandler handler) {
        notificationHandlers.add(handler);
    }

    private void notifyHandlers(Alert alert, AlertNotificationType type) {
        for (AlertNotificationHandler handler : notificationHandlers) {
            try {
                handler.handle(alert, type);
            } catch (Exception e) {
                log.error("Error in notification handler: {}", e.getMessage());
            }
        }
    }

    // Statistics

    public AlertStatistics getStatistics() {
        long total = alerts.size();
        long firing = countByStatus(AlertStatus.FIRING);
        long pending = countByStatus(AlertStatus.PENDING);
        long resolved = countByStatus(AlertStatus.RESOLVED);
        long acknowledged = countByStatus(AlertStatus.ACKNOWLEDGED);
        long silenced = countByStatus(AlertStatus.SILENCED);
        
        long critical = countBySeverity(AlertSeverity.CRITICAL);
        long warning = countBySeverity(AlertSeverity.WARNING);
        long info = countBySeverity(AlertSeverity.INFO);
        
        return new AlertStatistics(total, firing, pending, resolved, acknowledged, silenced,
                critical, warning, info, alertRules.size());
    }

    private long countByStatus(AlertStatus status) {
        return alerts.values().stream()
                .filter(a -> a.status() == status)
                .count();
    }

    private long countBySeverity(AlertSeverity severity) {
        return alerts.values().stream()
                .filter(a -> a.severity() == severity)
                .count();
    }

    // Maintenance

    public void pruneResolvedAlerts(Duration retention) {
        Instant threshold = Instant.now().minus(retention);
        alerts.entrySet().removeIf(e -> 
            e.getValue().status() == AlertStatus.RESOLVED &&
            e.getValue().endsAt() != null &&
            e.getValue().endsAt().isBefore(threshold)
        );
    }

    public void clear() {
        alerts.clear();
        alertRules.clear();
        pendingAlertStartTimes.clear();
    }

    // Inner types

    public enum AlertNotificationType {
        FIRING, RESOLVED, ACKNOWLEDGED
    }

    @FunctionalInterface
    public interface AlertNotificationHandler {
        void handle(Alert alert, AlertNotificationType type);
    }

    public record AlertStatistics(
            long total,
            long firing,
            long pending,
            long resolved,
            long acknowledged,
            long silenced,
            long critical,
            long warning,
            long info,
            long totalRules
    ) {}
}
