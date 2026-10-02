package com.portfolio.observability.health.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ServiceHealth Tests")
class ServiceHealthTest {

    @Test
    @DisplayName("Should create healthy service")
    void shouldCreateHealthyService() {
        ServiceHealth health = ServiceHealth.healthy("api-gateway", "1.0.0");

        assertThat(health.serviceName()).isEqualTo("api-gateway");
        assertThat(health.serviceVersion()).isEqualTo("1.0.0");
        assertThat(health.status()).isEqualTo(HealthStatus.UP);
        assertThat(health.isHealthy()).isTrue();
    }

    @Test
    @DisplayName("Should create unhealthy service")
    void shouldCreateUnhealthyService() {
        ServiceHealth health = ServiceHealth.unhealthy("failing-service", "2.0.0", "Database connection failed");

        assertThat(health.status()).isEqualTo(HealthStatus.DOWN);
        assertThat(health.isHealthy()).isFalse();
        assertThat(health.details()).containsEntry("reason", "Database connection failed");
    }

    @Test
    @DisplayName("Should aggregate component health - all up")
    void shouldAggregateComponentHealthAllUp() {
        List<ComponentHealth> components = List.of(
                ComponentHealth.up("database", Duration.ofMillis(10)),
                ComponentHealth.up("cache", Duration.ofMillis(2))
        );

        ServiceHealth health = ServiceHealth.aggregate("my-service", "1.0", components);

        assertThat(health.status()).isEqualTo(HealthStatus.UP);
        assertThat(health.components()).hasSize(2);
    }

    @Test
    @DisplayName("Should aggregate component health - degraded")
    void shouldAggregateComponentHealthDegraded() {
        List<ComponentHealth> components = List.of(
                ComponentHealth.up("database", Duration.ofMillis(10)),
                ComponentHealth.degraded("external-api", Duration.ofMillis(500), "Slow response")
        );

        ServiceHealth health = ServiceHealth.aggregate("my-service", "1.0", components);

        assertThat(health.status()).isEqualTo(HealthStatus.DEGRADED);
        assertThat(health.isDegraded()).isTrue();
    }

    @Test
    @DisplayName("Should aggregate component health - down")
    void shouldAggregateComponentHealthDown() {
        List<ComponentHealth> components = List.of(
                ComponentHealth.up("database", Duration.ofMillis(10)),
                ComponentHealth.down("cache", "Connection refused"),
                ComponentHealth.degraded("api", Duration.ofMillis(100), "Slow")
        );

        ServiceHealth health = ServiceHealth.aggregate("my-service", "1.0", components);

        assertThat(health.status()).isEqualTo(HealthStatus.DOWN);
    }

    @Test
    @DisplayName("Should handle empty components")
    void shouldHandleEmptyComponents() {
        ServiceHealth health = ServiceHealth.aggregate("my-service", "1.0", List.of());

        assertThat(health.status()).isEqualTo(HealthStatus.UP);
    }

    @Test
    @DisplayName("Should throw for null service name")
    void shouldThrowForNullServiceName() {
        assertThatThrownBy(() -> 
                ServiceHealth.healthy(null, "1.0"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw for blank service name")
    void shouldThrowForBlankServiceName() {
        assertThatThrownBy(() -> 
                ServiceHealth.healthy("  ", "1.0"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
