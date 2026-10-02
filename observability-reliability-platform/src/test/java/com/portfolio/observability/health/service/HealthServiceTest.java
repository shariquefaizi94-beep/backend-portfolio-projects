package com.portfolio.observability.health.service;

import com.portfolio.observability.health.model.ComponentHealth;
import com.portfolio.observability.health.model.HealthStatus;
import com.portfolio.observability.health.model.ServiceHealth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HealthService Tests")
class HealthServiceTest {

    private HealthService healthService;

    @BeforeEach
    void setUp() {
        healthService = new HealthService();
    }

    @Nested
    @DisplayName("Health Check Registration Tests")
    class HealthCheckRegistrationTests {

        @Test
        @DisplayName("Should register a health check")
        void shouldRegisterHealthCheck() {
            healthService.registerHealthCheck("database", 
                    () -> ComponentHealth.up("database", Duration.ofMillis(10)));

            List<String> checks = healthService.getRegisteredHealthChecks();

            assertThat(checks).contains("database");
        }

        @Test
        @DisplayName("Should register critical health check")
        void shouldRegisterCriticalHealthCheck() {
            healthService.registerHealthCheck("critical-db", 
                    () -> ComponentHealth.up("critical-db", Duration.ofMillis(5)),
                    Duration.ofSeconds(15),
                    true);

            assertThat(healthService.getRegisteredHealthChecks()).contains("critical-db");
        }

        @Test
        @DisplayName("Should unregister health check")
        void shouldUnregisterHealthCheck() {
            healthService.registerHealthCheck("temp-check", 
                    () -> ComponentHealth.up("temp-check", Duration.ofMillis(1)));
            
            healthService.unregisterHealthCheck("temp-check");

            assertThat(healthService.getRegisteredHealthChecks()).doesNotContain("temp-check");
        }
    }

    @Nested
    @DisplayName("Service Health Recording Tests")
    class ServiceHealthRecordingTests {

        @Test
        @DisplayName("Should record healthy service")
        void shouldRecordHealthyService() {
            ServiceHealth health = ServiceHealth.healthy("api-gateway", "1.0.0");

            ServiceHealth recorded = healthService.recordHealth(health);

            assertThat(recorded).isNotNull();
            assertThat(recorded.serviceName()).isEqualTo("api-gateway");
            assertThat(recorded.status()).isEqualTo(HealthStatus.UP);
        }

        @Test
        @DisplayName("Should record unhealthy service")
        void shouldRecordUnhealthyService() {
            ServiceHealth health = ServiceHealth.unhealthy("failing-service", "2.0.0", "Database connection failed");

            ServiceHealth recorded = healthService.recordHealth(health);

            assertThat(recorded.status()).isEqualTo(HealthStatus.DOWN);
        }

        @Test
        @DisplayName("Should record service health with components")
        void shouldRecordServiceHealthWithComponents() {
            List<ComponentHealth> components = List.of(
                    ComponentHealth.up("database", Duration.ofMillis(15)),
                    ComponentHealth.up("cache", Duration.ofMillis(2)),
                    ComponentHealth.degraded("external-api", Duration.ofMillis(500), "Slow response")
            );
            ServiceHealth health = ServiceHealth.aggregate("my-service", "3.0.0", components);

            healthService.recordHealth(health);
            var retrieved = healthService.getServiceHealth("my-service");

            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().status()).isEqualTo(HealthStatus.DEGRADED);
            assertThat(retrieved.get().components()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Service Health Query Tests")
    class ServiceHealthQueryTests {

        @BeforeEach
        void setUp() {
            healthService.recordHealth(ServiceHealth.healthy("service-a", "1.0"));
            healthService.recordHealth(ServiceHealth.healthy("service-b", "1.0"));
            healthService.recordHealth(ServiceHealth.unhealthy("service-c", "1.0", "Error"));
        }

        @Test
        @DisplayName("Should get service health by name")
        void shouldGetServiceHealthByName() {
            var health = healthService.getServiceHealth("service-a");

            assertThat(health).isPresent();
            assertThat(health.get().isHealthy()).isTrue();
        }

        @Test
        @DisplayName("Should get all service health")
        void shouldGetAllServiceHealth() {
            List<ServiceHealth> all = healthService.getAllServiceHealth();

            assertThat(all).hasSize(3);
        }

        @Test
        @DisplayName("Should get services by status")
        void shouldGetServicesByStatus() {
            List<ServiceHealth> healthy = healthService.getServicesByStatus(HealthStatus.UP);
            List<ServiceHealth> down = healthService.getServicesByStatus(HealthStatus.DOWN);

            assertThat(healthy).hasSize(2);
            assertThat(down).hasSize(1);
        }

        @Test
        @DisplayName("Should get unhealthy services")
        void shouldGetUnhealthyServices() {
            List<ServiceHealth> unhealthy = healthService.getUnhealthyServices();

            assertThat(unhealthy).hasSize(1);
            assertThat(unhealthy.get(0).serviceName()).isEqualTo("service-c");
        }

        @Test
        @DisplayName("Should get down services")
        void shouldGetDownServices() {
            List<ServiceHealth> down = healthService.getDownServices();

            assertThat(down).hasSize(1);
        }

        @Test
        @DisplayName("Should get service status map")
        void shouldGetServiceStatusMap() {
            Map<String, HealthStatus> statusMap = healthService.getServiceStatusMap();

            assertThat(statusMap).hasSize(3);
            assertThat(statusMap.get("service-a")).isEqualTo(HealthStatus.UP);
            assertThat(statusMap.get("service-c")).isEqualTo(HealthStatus.DOWN);
        }
    }

    @Nested
    @DisplayName("Health History Tests")
    class HealthHistoryTests {

        @Test
        @DisplayName("Should retrieve health history")
        void shouldRetrieveHealthHistory() {
            // Record multiple health states
            healthService.recordHealth(ServiceHealth.healthy("history-service", "1.0"));
            healthService.recordHealth(ServiceHealth.unhealthy("history-service", "1.0", "Error"));
            healthService.recordHealth(ServiceHealth.healthy("history-service", "1.0"));

            Instant start = Instant.now().minus(5, java.time.temporal.ChronoUnit.MINUTES);
            Instant end = Instant.now().plus(5, java.time.temporal.ChronoUnit.MINUTES);
            List<ServiceHealth> history = healthService.getHealthHistory("history-service", start, end);

            assertThat(history).hasSize(3);
        }

        @Test
        @DisplayName("Should calculate uptime percentage")
        void shouldCalculateUptimePercentage() {
            for (int i = 0; i < 8; i++) {
                healthService.recordHealth(ServiceHealth.healthy("uptime-service", "1.0"));
            }
            healthService.recordHealth(ServiceHealth.unhealthy("uptime-service", "1.0", "Error"));
            healthService.recordHealth(ServiceHealth.unhealthy("uptime-service", "1.0", "Error"));

            double uptime = healthService.calculateUptimePercentage("uptime-service", Duration.ofHours(1));

            assertThat(uptime).isEqualTo(80.0);
        }
    }

    @Nested
    @DisplayName("Component Health Tests")
    class ComponentHealthTests {

        @BeforeEach
        void setUp() {
            List<ComponentHealth> components = List.of(
                    ComponentHealth.up("database", Duration.ofMillis(10)),
                    ComponentHealth.down("cache", "Connection refused"),
                    ComponentHealth.degraded("api", Duration.ofMillis(300), "Slow")
            );
            healthService.recordHealth(ServiceHealth.aggregate("test-service", "1.0", components));
        }

        @Test
        @DisplayName("Should get unhealthy components")
        void shouldGetUnhealthyComponents() {
            List<ComponentHealth> unhealthy = healthService.getUnhealthyComponents();

            assertThat(unhealthy).hasSize(2);
        }

        @Test
        @DisplayName("Should get component health by service")
        void shouldGetComponentHealthByService() {
            Map<String, List<ComponentHealth>> byService = healthService.getComponentHealthByService();

            assertThat(byService).containsKey("test-service");
            assertThat(byService.get("test-service")).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Overall Health Summary Tests")
    class OverallHealthSummaryTests {

        @Test
        @DisplayName("Should return healthy summary when all services up")
        void shouldReturnHealthySummaryWhenAllServicesUp() {
            healthService.recordHealth(ServiceHealth.healthy("service-a", "1.0"));
            healthService.recordHealth(ServiceHealth.healthy("service-b", "1.0"));

            var summary = healthService.getOverallHealthSummary();

            assertThat(summary.status()).isEqualTo(HealthStatus.UP);
            assertThat(summary.healthScore()).isEqualTo(100.0);
            assertThat(summary.servicesUp()).isEqualTo(2);
            assertThat(summary.issues()).isEmpty();
        }

        @Test
        @DisplayName("Should return degraded summary when some services degraded")
        void shouldReturnDegradedSummaryWhenSomeServicesDegraded() {
            healthService.recordHealth(ServiceHealth.healthy("service-a", "1.0"));
            healthService.recordHealth(ServiceHealth.aggregate("service-b", "1.0", 
                    List.of(ComponentHealth.degraded("db", Duration.ofMillis(100), "Slow"))));

            var summary = healthService.getOverallHealthSummary();

            assertThat(summary.status()).isEqualTo(HealthStatus.DEGRADED);
            assertThat(summary.servicesDegraded()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return down summary when critical service down")
        void shouldReturnDownSummaryWhenServiceDown() {
            healthService.recordHealth(ServiceHealth.healthy("service-a", "1.0"));
            healthService.recordHealth(ServiceHealth.unhealthy("service-b", "1.0", "Critical failure"));

            var summary = healthService.getOverallHealthSummary();

            assertThat(summary.servicesDown()).isEqualTo(1);
            assertThat(summary.issues()).isNotEmpty();
        }

        @Test
        @DisplayName("Should return unknown when no services")
        void shouldReturnUnknownWhenNoServices() {
            var summary = healthService.getOverallHealthSummary();

            assertThat(summary.status()).isEqualTo(HealthStatus.UNKNOWN);
            assertThat(summary.healthScore()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @BeforeEach
        void setUp() {
            healthService.recordHealth(ServiceHealth.healthy("s1", "1.0"));
            healthService.recordHealth(ServiceHealth.healthy("s2", "1.0"));
            healthService.recordHealth(ServiceHealth.unhealthy("s3", "1.0", "Error"));
        }

        @Test
        @DisplayName("Should return correct statistics")
        void shouldReturnCorrectStatistics() {
            var stats = healthService.getStatistics();

            assertThat(stats.totalServices()).isEqualTo(3);
            assertThat(stats.servicesUp()).isEqualTo(2);
            assertThat(stats.servicesDown()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return status distribution")
        void shouldReturnStatusDistribution() {
            Map<HealthStatus, Long> distribution = healthService.getStatusDistribution();

            assertThat(distribution.get(HealthStatus.UP)).isEqualTo(2);
            assertThat(distribution.get(HealthStatus.DOWN)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Maintenance Tests")
    class MaintenanceTests {

        @Test
        @DisplayName("Should remove service")
        void shouldRemoveService() {
            healthService.recordHealth(ServiceHealth.healthy("to-remove", "1.0"));
            
            healthService.removeService("to-remove");

            assertThat(healthService.getServiceHealth("to-remove")).isEmpty();
        }

        @Test
        @DisplayName("Should clear all data")
        void shouldClearAllData() {
            healthService.recordHealth(ServiceHealth.healthy("s1", "1.0"));
            healthService.recordHealth(ServiceHealth.healthy("s2", "1.0"));
            healthService.registerHealthCheck("check", () -> ComponentHealth.up("check", Duration.ZERO));

            healthService.clear();

            assertThat(healthService.getAllServiceHealth()).isEmpty();
            assertThat(healthService.getRegisteredHealthChecks()).isEmpty();
        }
    }
}
