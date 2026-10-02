package com.nexaforge.analytics.controller;

import com.nexaforge.analytics.model.AggregatedMetrics;
import com.nexaforge.analytics.model.DashboardSummary;
import com.nexaforge.analytics.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalyticsController REST endpoints.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    private AnalyticsController controller;

    @BeforeEach
    void setUp() {
        controller = new AnalyticsController(analyticsService);
    }

    @Test
    void getDashboard_returnsOkWithSummary() {
        DashboardSummary summary = new DashboardSummary(
                5000L, 500L, 750L, 25.0,
                Map.of("click", 2500L, "view", 2500L),
                Map.of("US", 3000L, "UK", 2000L),
                Map.of("desktop", 3500L, "mobile", 1500L),
                10, Instant.now()
        );
        when(analyticsService.getDashboardSummary(10)).thenReturn(summary);

        ResponseEntity<DashboardSummary> response = controller.getDashboard(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(summary, response.getBody());
        verify(analyticsService).getDashboardSummary(10);
    }

    @Test
    void getDashboard_usesDefaultWindowCount() {
        DashboardSummary summary = new DashboardSummary(
                1000L, 100L, 150L, 20.0,
                Map.of(), Map.of(), Map.of(),
                10, Instant.now()
        );
        when(analyticsService.getDashboardSummary(10)).thenReturn(summary);

        ResponseEntity<DashboardSummary> response = controller.getDashboard(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(analyticsService).getDashboardSummary(10);
    }

    @Test
    void getDashboard_respectsCustomWindowCount() {
        DashboardSummary summary = new DashboardSummary(
                2000L, 200L, 300L, 15.0,
                Map.of(), Map.of(), Map.of(),
                25, Instant.now()
        );
        when(analyticsService.getDashboardSummary(25)).thenReturn(summary);

        ResponseEntity<DashboardSummary> response = controller.getDashboard(25);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(25, response.getBody().windowsAggregated());
    }

    @Test
    void getRecentMetrics_returnsOkWithList() {
        List<AggregatedMetrics> metrics = List.of(
                createTestMetrics("window-1"),
                createTestMetrics("window-2")
        );
        when(analyticsService.getRecentMetrics(20)).thenReturn(metrics);

        ResponseEntity<List<AggregatedMetrics>> response = controller.getRecentMetrics(20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void getRecentMetrics_returnsEmptyListWhenNoData() {
        when(analyticsService.getRecentMetrics(20)).thenReturn(List.of());

        ResponseEntity<List<AggregatedMetrics>> response = controller.getRecentMetrics(20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getRecentMetrics_respectsCustomCount() {
        when(analyticsService.getRecentMetrics(50)).thenReturn(List.of());

        controller.getRecentMetrics(50);

        verify(analyticsService).getRecentMetrics(50);
    }

    private AggregatedMetrics createTestMetrics(String windowId) {
        return new AggregatedMetrics(
                windowId,
                Instant.now().minusSeconds(60),
                Instant.now(),
                1000L, 100L, 150L,
                Map.of("click", 500L),
                Map.of("US", 600L),
                Map.of("desktop", 700L),
                Map.of("/home", 400L),
                20.0, 75L, Instant.now()
        );
    }
}
