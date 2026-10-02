package com.portfolio.observability.alerting.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AlertRule Tests")
class AlertRuleTest {

    @Test
    @DisplayName("Should create alert rule")
    void shouldCreateAlertRule() {
        AlertRule rule = AlertRule.create(
                "HighErrorRate",
                "Error rate exceeds 5%",
                "error_rate > 5",
                Duration.ofMinutes(5),
                AlertSeverity.CRITICAL
        );

        assertThat(rule.name()).isEqualTo("HighErrorRate");
        assertThat(rule.expression()).isEqualTo("error_rate > 5");
        assertThat(rule.forDuration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(rule.severity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(rule.enabled()).isTrue();
    }

    @Test
    @DisplayName("Should create rule with zero for duration when null")
    void shouldCreateRuleWithZeroForDurationWhenNull() {
        AlertRule rule = AlertRule.create(
                "InstantAlert", "desc", "metric > 100", null, AlertSeverity.WARNING);

        assertThat(rule.forDuration()).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("Should add labels")
    void shouldAddLabels() {
        AlertRule rule = AlertRule.create("Test", "d", "m>1", null, AlertSeverity.INFO);

        AlertRule withLabels = rule.withLabels(Map.of("team", "platform", "env", "prod"));

        assertThat(withLabels.labels()).containsEntry("team", "platform");
        assertThat(withLabels.labels()).containsEntry("env", "prod");
    }

    @Test
    @DisplayName("Should add annotations")
    void shouldAddAnnotations() {
        AlertRule rule = AlertRule.create("Test", "d", "m>1", null, AlertSeverity.INFO);

        AlertRule withAnnotations = rule.withAnnotations(Map.of("summary", "Alert summary"));

        assertThat(withAnnotations.annotations()).containsEntry("summary", "Alert summary");
    }

    @Test
    @DisplayName("Should enable rule")
    void shouldEnableRule() {
        AlertRule rule = AlertRule.create("Test", "d", "m>1", null, AlertSeverity.INFO).disable();

        AlertRule enabled = rule.enable();

        assertThat(enabled.enabled()).isTrue();
    }

    @Test
    @DisplayName("Should disable rule")
    void shouldDisableRule() {
        AlertRule rule = AlertRule.create("Test", "d", "m>1", null, AlertSeverity.INFO);

        AlertRule disabled = rule.disable();

        assertThat(disabled.enabled()).isFalse();
    }

    @Test
    @DisplayName("Should throw for null name")
    void shouldThrowForNullName() {
        assertThatThrownBy(() -> 
                AlertRule.create(null, "d", "m>1", null, AlertSeverity.INFO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw for null expression")
    void shouldThrowForNullExpression() {
        assertThatThrownBy(() -> 
                AlertRule.create("Test", "d", null, null, AlertSeverity.INFO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw for null severity")
    void shouldThrowForNullSeverity() {
        assertThatThrownBy(() -> 
                AlertRule.create("Test", "d", "m>1", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
