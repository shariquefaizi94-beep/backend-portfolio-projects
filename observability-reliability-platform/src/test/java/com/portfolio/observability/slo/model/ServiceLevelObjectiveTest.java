package com.portfolio.observability.slo.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ServiceLevelObjective Tests")
class ServiceLevelObjectiveTest {

    @Test
    @DisplayName("Should create availability SLO")
    void shouldCreateAvailabilitySlo() {
        ServiceLevelObjective slo = ServiceLevelObjective.availability(
                "API Availability", "api-gateway", 99.9, Duration.ofDays(30));

        assertThat(slo.name()).isEqualTo("API Availability");
        assertThat(slo.service()).isEqualTo("api-gateway");
        assertThat(slo.type()).isEqualTo(SloType.AVAILABILITY);
        assertThat(slo.target()).isEqualTo(99.9);
        assertThat(slo.window()).isEqualTo(Duration.ofDays(30));
        assertThat(slo.enabled()).isTrue();
        assertThat(slo.sliExpression()).isNotBlank();
    }

    @Test
    @DisplayName("Should create latency SLO")
    void shouldCreateLatencySlo() {
        ServiceLevelObjective slo = ServiceLevelObjective.latency(
                "API Latency P95", "api-gateway", 95.0, Duration.ofDays(7), Duration.ofMillis(200));

        assertThat(slo.type()).isEqualTo(SloType.LATENCY);
        assertThat(slo.labels()).containsEntry("threshold_ms", "200");
    }

    @Test
    @DisplayName("Should calculate error budget percentage")
    void shouldCalculateErrorBudgetPercentage() {
        ServiceLevelObjective slo = ServiceLevelObjective.availability(
                "Test", "svc", 99.9, Duration.ofDays(30));

        double errorBudget = slo.errorBudgetPercentage();

        assertThat(errorBudget).isCloseTo(0.1, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    @DisplayName("Should calculate error budget duration")
    void shouldCalculateErrorBudgetDuration() {
        ServiceLevelObjective slo = ServiceLevelObjective.availability(
                "Test", "svc", 99.0, Duration.ofDays(30));

        Duration errorBudget = slo.errorBudgetDuration();

        // 1% of 30 days = 7.2 hours = 432 minutes
        assertThat(errorBudget.toMinutes()).isEqualTo(432);
    }

    @Test
    @DisplayName("Should throw for null name")
    void shouldThrowForNullName() {
        assertThatThrownBy(() -> 
                ServiceLevelObjective.availability(null, "svc", 99.0, Duration.ofDays(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw for null service")
    void shouldThrowForNullService() {
        assertThatThrownBy(() -> 
                ServiceLevelObjective.availability("Test", null, 99.0, Duration.ofDays(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw for invalid target below 0")
    void shouldThrowForInvalidTargetBelowZero() {
        assertThatThrownBy(() -> 
                ServiceLevelObjective.availability("Test", "svc", -1.0, Duration.ofDays(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw for invalid target above 100")
    void shouldThrowForInvalidTargetAbove100() {
        assertThatThrownBy(() -> 
                ServiceLevelObjective.availability("Test", "svc", 101.0, Duration.ofDays(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw for negative window")
    void shouldThrowForNegativeWindow() {
        assertThatThrownBy(() -> 
                ServiceLevelObjective.availability("Test", "svc", 99.0, Duration.ofDays(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
