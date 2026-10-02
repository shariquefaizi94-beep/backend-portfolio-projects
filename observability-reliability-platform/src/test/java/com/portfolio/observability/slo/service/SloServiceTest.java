package com.portfolio.observability.slo.service;

import com.portfolio.observability.metrics.service.MetricsService;
import com.portfolio.observability.slo.model.ServiceLevelObjective;
import com.portfolio.observability.slo.model.SloState;
import com.portfolio.observability.slo.model.SloStatus;
import com.portfolio.observability.slo.model.SloType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SloService Tests")
class SloServiceTest {

    @Mock
    private MetricsService metricsService;

    private SloService sloService;

    @BeforeEach
    void setUp() {
        sloService = new SloService(metricsService);
    }

    @Nested
    @DisplayName("SLO Definition Tests")
    class SloDefinitionTests {

        @Test
        @DisplayName("Should create an availability SLO")
        void shouldCreateAvailabilitySlo() {
            ServiceLevelObjective slo = sloService.createAvailabilitySlo(
                    "API Gateway Availability",
                    "api-gateway",
                    99.9,
                    Duration.ofDays(30)
            );

            assertThat(slo).isNotNull();
            assertThat(slo.name()).isEqualTo("API Gateway Availability");
            assertThat(slo.service()).isEqualTo("api-gateway");
            assertThat(slo.type()).isEqualTo(SloType.AVAILABILITY);
            assertThat(slo.target()).isEqualTo(99.9);
            assertThat(slo.enabled()).isTrue();
        }

        @Test
        @DisplayName("Should create a latency SLO")
        void shouldCreateLatencySlo() {
            ServiceLevelObjective slo = sloService.createLatencySlo(
                    "API Latency P95",
                    "api-gateway",
                    95.0,
                    Duration.ofDays(7),
                    Duration.ofMillis(200)
            );

            assertThat(slo).isNotNull();
            assertThat(slo.type()).isEqualTo(SloType.LATENCY);
            assertThat(slo.labels()).containsKey("threshold_ms");
        }

        @Test
        @DisplayName("Should create a custom SLO")
        void shouldCreateCustomSlo() {
            ServiceLevelObjective slo = ServiceLevelObjective.availability(
                    "Custom SLO", "my-service", 99.5, Duration.ofDays(14));

            ServiceLevelObjective created = sloService.createSlo(slo);

            assertThat(created).isNotNull();
            assertThat(created.id()).isNotNull();
        }

        @Test
        @DisplayName("Should retrieve SLO by ID")
        void shouldRetrieveSloById() {
            ServiceLevelObjective slo = sloService.createAvailabilitySlo(
                    "Test SLO", "test-service", 99.0, Duration.ofDays(30));

            var retrieved = sloService.getSlo(slo.id());

            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().name()).isEqualTo("Test SLO");
        }

        @Test
        @DisplayName("Should retrieve SLO by name")
        void shouldRetrieveSloByName() {
            sloService.createAvailabilitySlo(
                    "Unique SLO Name", "service", 99.0, Duration.ofDays(30));

            var retrieved = sloService.getSloByName("Unique SLO Name");

            assertThat(retrieved).isPresent();
        }

        @Test
        @DisplayName("Should get all SLOs")
        void shouldGetAllSlos() {
            sloService.createAvailabilitySlo("SLO1", "s1", 99.0, Duration.ofDays(30));
            sloService.createAvailabilitySlo("SLO2", "s2", 99.5, Duration.ofDays(30));

            List<ServiceLevelObjective> all = sloService.getAllSlos();

            assertThat(all).hasSize(2);
        }

        @Test
        @DisplayName("Should get SLOs by service")
        void shouldGetSlosByService() {
            sloService.createAvailabilitySlo("SLO1", "service-a", 99.0, Duration.ofDays(30));
            sloService.createAvailabilitySlo("SLO2", "service-a", 99.5, Duration.ofDays(30));
            sloService.createAvailabilitySlo("SLO3", "service-b", 99.0, Duration.ofDays(30));

            List<ServiceLevelObjective> serviceASlos = sloService.getSlosByService("service-a");

            assertThat(serviceASlos).hasSize(2);
        }

        @Test
        @DisplayName("Should get SLOs by type")
        void shouldGetSlosByType() {
            sloService.createAvailabilitySlo("Avail SLO", "s", 99.0, Duration.ofDays(30));
            sloService.createLatencySlo("Latency SLO", "s", 95.0, Duration.ofDays(7), Duration.ofMillis(200));

            List<ServiceLevelObjective> availSlos = sloService.getSlosByType(SloType.AVAILABILITY);
            List<ServiceLevelObjective> latencySlos = sloService.getSlosByType(SloType.LATENCY);

            assertThat(availSlos).hasSize(1);
            assertThat(latencySlos).hasSize(1);
        }

        @Test
        @DisplayName("Should delete SLO")
        void shouldDeleteSlo() {
            ServiceLevelObjective slo = sloService.createAvailabilitySlo(
                    "ToDelete", "s", 99.0, Duration.ofDays(30));

            sloService.deleteSlo(slo.id());

            assertThat(sloService.getSlo(slo.id())).isEmpty();
        }
    }

    @Nested
    @DisplayName("SLO Status Calculation Tests")
    class SloStatusCalculationTests {

        @Test
        @DisplayName("Should calculate status for availability SLO meeting target")
        void shouldCalculateStatusForMeetingSlo() {
            ServiceLevelObjective slo = sloService.createAvailabilitySlo(
                    "High Availability", "api", 99.9, Duration.ofDays(30));
            
            // Mock 100% availability (no errors)
            when(metricsService.calculateSum(eq("api_requests_total"), any(Duration.class)))
                    .thenReturn(10000.0);
            when(metricsService.calculateSum(eq("api_requests_errors"), any(Duration.class)))
                    .thenReturn(0.0);

            SloStatus status = sloService.calculateStatus(slo.id());

            assertThat(status.state()).isEqualTo(SloState.MET);
            assertThat(status.currentValue()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should calculate status for breached SLO")
        void shouldCalculateStatusForBreachedSlo() {
            ServiceLevelObjective slo = sloService.createAvailabilitySlo(
                    "Availability", "api", 99.9, Duration.ofDays(30));
            
            // Mock 99% availability (1% errors - above 0.1% budget)
            when(metricsService.calculateSum(eq("api_requests_total"), any(Duration.class)))
                    .thenReturn(10000.0);
            when(metricsService.calculateSum(eq("api_requests_errors"), any(Duration.class)))
                    .thenReturn(100.0);

            SloStatus status = sloService.calculateStatus(slo.id());

            assertThat(status.currentValue()).isEqualTo(99.0);
            // Status depends on how much budget is consumed
        }

        @Test
        @DisplayName("Should throw for non-existent SLO")
        void shouldThrowForNonExistentSlo() {
            UUID randomId = UUID.randomUUID();

            assertThatThrownBy(() -> sloService.calculateStatus(randomId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should calculate all statuses")
        void shouldCalculateAllStatuses() {
            sloService.createAvailabilitySlo("SLO1", "s1", 99.0, Duration.ofDays(30));
            sloService.createAvailabilitySlo("SLO2", "s2", 99.0, Duration.ofDays(30));
            
            when(metricsService.calculateSum(any(), any(Duration.class))).thenReturn(1000.0);

            List<SloStatus> statuses = sloService.calculateAllStatuses();

            assertThat(statuses).hasSize(2);
        }
    }

    @Nested
    @DisplayName("SLO Status Query Tests")
    class SloStatusQueryTests {

        private ServiceLevelObjective testSlo;

        @BeforeEach
        void setUp() {
            testSlo = sloService.createAvailabilitySlo(
                    "Test SLO", "test", 99.0, Duration.ofDays(30));
            when(metricsService.calculateSum(any(), any(Duration.class))).thenReturn(1000.0);
            sloService.calculateStatus(testSlo.id());
        }

        @Test
        @DisplayName("Should get latest status")
        void shouldGetLatestStatus() {
            var latest = sloService.getLatestStatus(testSlo.id());

            assertThat(latest).isPresent();
            assertThat(latest.get().sloName()).isEqualTo("Test SLO");
        }

        @Test
        @DisplayName("Should get all latest statuses")
        void shouldGetAllLatestStatuses() {
            sloService.createAvailabilitySlo("SLO2", "s2", 99.0, Duration.ofDays(30));

            List<SloStatus> latest = sloService.getAllLatestStatuses();

            assertThat(latest).hasSize(1); // Only one has status calculated
        }
    }

    @Nested
    @DisplayName("Error Budget Tests")
    class ErrorBudgetTests {

        @Test
        @DisplayName("Should calculate error budget summary")
        void shouldCalculateErrorBudgetSummary() {
            ServiceLevelObjective slo = sloService.createAvailabilitySlo(
                    "Budget Test", "api", 99.9, Duration.ofDays(30));
            
            when(metricsService.calculateSum(any(), any(Duration.class))).thenReturn(10000.0);
            sloService.calculateStatus(slo.id());

            var summary = sloService.getErrorBudgetSummary(slo.id());

            assertThat(summary).isNotNull();
            assertThat(summary.sloName()).isEqualTo("Budget Test");
            assertThat(summary.target()).isEqualTo(99.9);
            assertThat(summary.totalBudgetMinutes()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should throw for non-existent SLO error budget")
        void shouldThrowForNonExistentSloErrorBudget() {
            UUID randomId = UUID.randomUUID();

            assertThatThrownBy(() -> sloService.getErrorBudgetSummary(randomId))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("SLO Statistics Tests")
    class SloStatisticsTests {

        @Test
        @DisplayName("Should return correct statistics")
        void shouldReturnCorrectStatistics() {
            sloService.createAvailabilitySlo("SLO1", "s1", 99.0, Duration.ofDays(30));
            sloService.createAvailabilitySlo("SLO2", "s2", 99.5, Duration.ofDays(30));

            var stats = sloService.getStatistics();

            assertThat(stats.total()).isEqualTo(2);
            assertThat(stats.enabled()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return empty statistics when no SLOs")
        void shouldReturnEmptyStatisticsWhenNoSlos() {
            var stats = sloService.getStatistics();

            assertThat(stats.total()).isZero();
            assertThat(stats.overallCompliance()).isEqualTo(100.0);
        }
    }
}
