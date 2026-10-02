package com.portfolio.observability.alerting.service;

import com.portfolio.observability.alerting.model.Alert;
import com.portfolio.observability.alerting.model.AlertRule;
import com.portfolio.observability.alerting.model.AlertSeverity;
import com.portfolio.observability.alerting.model.AlertStatus;
import com.portfolio.observability.metrics.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertingService Tests")
class AlertingServiceTest {

    @Mock
    private MetricsService metricsService;

    private AlertingService alertingService;

    @BeforeEach
    void setUp() {
        alertingService = new AlertingService(metricsService);
    }

    @Nested
    @DisplayName("Alert Rule Management Tests")
    class AlertRuleManagementTests {

        @Test
        @DisplayName("Should create an alert rule")
        void shouldCreateAlertRule() {
            AlertRule rule = AlertRule.create(
                    "HighCpuUsage",
                    "CPU usage exceeds 90%",
                    "cpu_usage > 90",
                    Duration.ofMinutes(5),
                    AlertSeverity.WARNING
            );

            AlertRule created = alertingService.createRule(rule);

            assertThat(created).isNotNull();
            assertThat(created.name()).isEqualTo("HighCpuUsage");
            assertThat(created.severity()).isEqualTo(AlertSeverity.WARNING);
            assertThat(created.enabled()).isTrue();
        }

        @Test
        @DisplayName("Should retrieve alert rule by ID")
        void shouldRetrieveAlertRuleById() {
            AlertRule rule = AlertRule.create(
                    "TestRule", "desc", "metric > 100", null, AlertSeverity.INFO);
            alertingService.createRule(rule);

            var retrieved = alertingService.getRule(rule.id());

            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().name()).isEqualTo("TestRule");
        }

        @Test
        @DisplayName("Should retrieve alert rule by name")
        void shouldRetrieveAlertRuleByName() {
            AlertRule rule = AlertRule.create(
                    "UniqueRuleName", "desc", "metric > 100", null, AlertSeverity.CRITICAL);
            alertingService.createRule(rule);

            var retrieved = alertingService.getRuleByName("UniqueRuleName");

            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().severity()).isEqualTo(AlertSeverity.CRITICAL);
        }

        @Test
        @DisplayName("Should get all rules")
        void shouldGetAllRules() {
            alertingService.createRule(AlertRule.create("Rule1", "d", "m>1", null, AlertSeverity.INFO));
            alertingService.createRule(AlertRule.create("Rule2", "d", "m>2", null, AlertSeverity.WARNING));

            List<AlertRule> rules = alertingService.getAllRules();

            assertThat(rules).hasSize(2);
        }

        @Test
        @DisplayName("Should get only enabled rules")
        void shouldGetOnlyEnabledRules() {
            AlertRule rule1 = AlertRule.create("Rule1", "d", "m>1", null, AlertSeverity.INFO);
            AlertRule rule2 = AlertRule.create("Rule2", "d", "m>2", null, AlertSeverity.INFO);
            alertingService.createRule(rule1);
            alertingService.createRule(rule2);
            alertingService.disableRule(rule2.id());

            List<AlertRule> enabled = alertingService.getEnabledRules();

            assertThat(enabled).hasSize(1);
            assertThat(enabled.get(0).name()).isEqualTo("Rule1");
        }

        @Test
        @DisplayName("Should enable a rule")
        void shouldEnableRule() {
            AlertRule rule = AlertRule.create("Rule", "d", "m>1", null, AlertSeverity.INFO);
            alertingService.createRule(rule);
            alertingService.disableRule(rule.id());

            AlertRule enabled = alertingService.enableRule(rule.id());

            assertThat(enabled.enabled()).isTrue();
        }

        @Test
        @DisplayName("Should disable a rule")
        void shouldDisableRule() {
            AlertRule rule = AlertRule.create("Rule", "d", "m>1", null, AlertSeverity.INFO);
            alertingService.createRule(rule);

            AlertRule disabled = alertingService.disableRule(rule.id());

            assertThat(disabled.enabled()).isFalse();
        }

        @Test
        @DisplayName("Should throw when enabling non-existent rule")
        void shouldThrowWhenEnablingNonExistentRule() {
            UUID randomId = UUID.randomUUID();

            assertThatThrownBy(() -> alertingService.enableRule(randomId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should delete a rule")
        void shouldDeleteRule() {
            AlertRule rule = AlertRule.create("ToDelete", "d", "m>1", null, AlertSeverity.INFO);
            alertingService.createRule(rule);

            alertingService.deleteRule(rule.id());

            assertThat(alertingService.getRule(rule.id())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Alert Management Tests")
    class AlertManagementTests {

        @Test
        @DisplayName("Should create an alert")
        void shouldCreateAlert() {
            Alert alert = alertingService.createAlert(
                    "HighMemoryUsage",
                    "Memory usage is above threshold",
                    AlertSeverity.WARNING,
                    Map.of("service", "api-gateway"),
                    Map.of("runbook", "https://docs/runbook")
            );

            assertThat(alert).isNotNull();
            assertThat(alert.name()).isEqualTo("HighMemoryUsage");
            assertThat(alert.severity()).isEqualTo(AlertSeverity.WARNING);
            assertThat(alert.status()).isEqualTo(AlertStatus.PENDING);
        }

        @Test
        @DisplayName("Should get alert by ID")
        void shouldGetAlertById() {
            Alert alert = alertingService.createAlert(
                    "TestAlert", "desc", AlertSeverity.INFO, Map.of(), Map.of());

            var retrieved = alertingService.getAlert(alert.id());

            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().name()).isEqualTo("TestAlert");
        }

        @Test
        @DisplayName("Should get all alerts")
        void shouldGetAllAlerts() {
            alertingService.createAlert("Alert1", "d", AlertSeverity.INFO, Map.of(), Map.of());
            alertingService.createAlert("Alert2", "d", AlertSeverity.WARNING, Map.of(), Map.of());

            List<Alert> alerts = alertingService.getAllAlerts();

            assertThat(alerts).hasSize(2);
        }

        @Test
        @DisplayName("Should get alerts by status")
        void shouldGetAlertsByStatus() {
            Alert alert1 = alertingService.createAlert("A1", "d", AlertSeverity.INFO, Map.of(), Map.of());
            alertingService.createAlert("A2", "d", AlertSeverity.INFO, Map.of(), Map.of());
            alertingService.fireAlert(alert1.id());

            List<Alert> firing = alertingService.getAlertsByStatus(AlertStatus.FIRING);

            assertThat(firing).hasSize(1);
            assertThat(firing.get(0).name()).isEqualTo("A1");
        }

        @Test
        @DisplayName("Should get alerts by severity")
        void shouldGetAlertsBySeverity() {
            alertingService.createAlert("A1", "d", AlertSeverity.CRITICAL, Map.of(), Map.of());
            alertingService.createAlert("A2", "d", AlertSeverity.WARNING, Map.of(), Map.of());
            alertingService.createAlert("A3", "d", AlertSeverity.CRITICAL, Map.of(), Map.of());

            List<Alert> critical = alertingService.getAlertsBySeverity(AlertSeverity.CRITICAL);

            assertThat(critical).hasSize(2);
        }

        @Test
        @DisplayName("Should get active alerts")
        void shouldGetActiveAlerts() {
            Alert pending = alertingService.createAlert("Pending", "d", AlertSeverity.INFO, Map.of(), Map.of());
            Alert firing = alertingService.createAlert("Firing", "d", AlertSeverity.INFO, Map.of(), Map.of());
            Alert resolved = alertingService.createAlert("Resolved", "d", AlertSeverity.INFO, Map.of(), Map.of());
            
            alertingService.fireAlert(firing.id());
            alertingService.resolveAlert(resolved.id());

            List<Alert> active = alertingService.getActiveAlerts();

            assertThat(active).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Alert State Transition Tests")
    class AlertStateTransitionTests {

        private Alert testAlert;

        @BeforeEach
        void setUp() {
            testAlert = alertingService.createAlert(
                    "TestAlert", "desc", AlertSeverity.WARNING, Map.of(), Map.of());
        }

        @Test
        @DisplayName("Should fire an alert")
        void shouldFireAlert() {
            Alert fired = alertingService.fireAlert(testAlert.id());

            assertThat(fired.status()).isEqualTo(AlertStatus.FIRING);
            assertThat(fired.isFiring()).isTrue();
        }

        @Test
        @DisplayName("Should resolve an alert")
        void shouldResolveAlert() {
            alertingService.fireAlert(testAlert.id());
            
            Alert resolved = alertingService.resolveAlert(testAlert.id());

            assertThat(resolved.status()).isEqualTo(AlertStatus.RESOLVED);
            assertThat(resolved.isResolved()).isTrue();
            assertThat(resolved.endsAt()).isNotNull();
        }

        @Test
        @DisplayName("Should acknowledge an alert")
        void shouldAcknowledgeAlert() {
            alertingService.fireAlert(testAlert.id());
            
            Alert acknowledged = alertingService.acknowledgeAlert(testAlert.id());

            assertThat(acknowledged.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        }

        @Test
        @DisplayName("Should silence an alert")
        void shouldSilenceAlert() {
            alertingService.fireAlert(testAlert.id());
            
            Alert silenced = alertingService.silenceAlert(testAlert.id());

            assertThat(silenced.status()).isEqualTo(AlertStatus.SILENCED);
        }

        @Test
        @DisplayName("Should throw when firing non-existent alert")
        void shouldThrowWhenFiringNonExistentAlert() {
            UUID randomId = UUID.randomUUID();

            assertThatThrownBy(() -> alertingService.fireAlert(randomId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("Alert Statistics Tests")
    class AlertStatisticsTests {

        @BeforeEach
        void setUp() {
            // Create alerts with different statuses and severities
            Alert a1 = alertingService.createAlert("A1", "d", AlertSeverity.CRITICAL, Map.of(), Map.of());
            Alert a2 = alertingService.createAlert("A2", "d", AlertSeverity.WARNING, Map.of(), Map.of());
            Alert a3 = alertingService.createAlert("A3", "d", AlertSeverity.INFO, Map.of(), Map.of());
            
            alertingService.fireAlert(a1.id());
            alertingService.fireAlert(a2.id());
            alertingService.resolveAlert(a3.id());
        }

        @Test
        @DisplayName("Should return correct statistics")
        void shouldReturnCorrectStatistics() {
            var stats = alertingService.getStatistics();

            assertThat(stats.total()).isEqualTo(3);
            assertThat(stats.firing()).isEqualTo(2);
            assertThat(stats.resolved()).isEqualTo(1);
            assertThat(stats.critical()).isEqualTo(1);
            assertThat(stats.warning()).isEqualTo(1);
            assertThat(stats.info()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Alert Notification Tests")
    class AlertNotificationTests {

        @Test
        @DisplayName("Should notify handlers when alert fires")
        void shouldNotifyHandlersWhenAlertFires() {
            boolean[] handlerCalled = {false};
            alertingService.registerNotificationHandler((alert, type) -> {
                if (type == AlertingService.AlertNotificationType.FIRING) {
                    handlerCalled[0] = true;
                }
            });
            
            Alert alert = alertingService.createAlert("Test", "d", AlertSeverity.INFO, Map.of(), Map.of());
            alertingService.fireAlert(alert.id());

            assertThat(handlerCalled[0]).isTrue();
        }

        @Test
        @DisplayName("Should notify handlers when alert resolves")
        void shouldNotifyHandlersWhenAlertResolves() {
            boolean[] handlerCalled = {false};
            alertingService.registerNotificationHandler((alert, type) -> {
                if (type == AlertingService.AlertNotificationType.RESOLVED) {
                    handlerCalled[0] = true;
                }
            });
            
            Alert alert = alertingService.createAlert("Test", "d", AlertSeverity.INFO, Map.of(), Map.of());
            alertingService.fireAlert(alert.id());
            alertingService.resolveAlert(alert.id());

            assertThat(handlerCalled[0]).isTrue();
        }
    }
}
