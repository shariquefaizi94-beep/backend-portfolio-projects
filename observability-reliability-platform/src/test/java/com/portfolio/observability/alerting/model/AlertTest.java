package com.portfolio.observability.alerting.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Alert Tests")
class AlertTest {

    @Test
    @DisplayName("Should create alert with factory method")
    void shouldCreateAlertWithFactoryMethod() {
        Alert alert = Alert.create(
                "HighCpuUsage",
                "CPU usage exceeds 90%",
                AlertSeverity.WARNING,
                Map.of("service", "api-gateway"),
                Map.of("runbook", "https://docs/runbook")
        );

        assertThat(alert.name()).isEqualTo("HighCpuUsage");
        assertThat(alert.severity()).isEqualTo(AlertSeverity.WARNING);
        assertThat(alert.status()).isEqualTo(AlertStatus.PENDING);
        assertThat(alert.labels()).containsEntry("service", "api-gateway");
        assertThat(alert.startsAt()).isNotNull();
    }

    @Test
    @DisplayName("Should fire alert")
    void shouldFireAlert() {
        Alert alert = Alert.create("Test", "desc", AlertSeverity.INFO, null, null);

        Alert fired = alert.fire();

        assertThat(fired.status()).isEqualTo(AlertStatus.FIRING);
        assertThat(fired.isFiring()).isTrue();
        assertThat(fired.lastUpdatedAt()).isAfterOrEqualTo(alert.lastUpdatedAt());
    }

    @Test
    @DisplayName("Should resolve alert")
    void shouldResolveAlert() {
        Alert alert = Alert.create("Test", "desc", AlertSeverity.INFO, null, null).fire();

        Alert resolved = alert.resolve();

        assertThat(resolved.status()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(resolved.isResolved()).isTrue();
        assertThat(resolved.endsAt()).isNotNull();
    }

    @Test
    @DisplayName("Should acknowledge alert")
    void shouldAcknowledgeAlert() {
        Alert alert = Alert.create("Test", "desc", AlertSeverity.INFO, null, null).fire();

        Alert acknowledged = alert.acknowledge();

        assertThat(acknowledged.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    }

    @Test
    @DisplayName("Should silence alert")
    void shouldSilenceAlert() {
        Alert alert = Alert.create("Test", "desc", AlertSeverity.INFO, null, null).fire();

        Alert silenced = alert.silence();

        assertThat(silenced.status()).isEqualTo(AlertStatus.SILENCED);
    }

    @Test
    @DisplayName("Should calculate duration")
    void shouldCalculateDuration() {
        Alert alert = Alert.create("Test", "desc", AlertSeverity.INFO, null, null);

        assertThat(alert.duration()).isNotNull();
        assertThat(alert.duration().toNanos()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should throw for null name")
    void shouldThrowForNullName() {
        assertThatThrownBy(() -> 
                Alert.create(null, "desc", AlertSeverity.INFO, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw for null severity")
    void shouldThrowForNullSeverity() {
        assertThatThrownBy(() -> 
                Alert.create("Test", "desc", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
