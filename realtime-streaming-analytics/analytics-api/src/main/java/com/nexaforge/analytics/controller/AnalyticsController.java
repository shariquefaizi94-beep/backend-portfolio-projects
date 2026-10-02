package com.nexaforge.analytics.controller;

import com.nexaforge.analytics.model.AggregatedMetrics;
import com.nexaforge.analytics.model.DashboardSummary;
import com.nexaforge.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for querying real-time analytics.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Real-time streaming analytics queries")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard summary aggregating recent time windows")
    public ResponseEntity<DashboardSummary> getDashboard(
            @RequestParam(defaultValue = "10") int windows) {
        return ResponseEntity.ok(analyticsService.getDashboardSummary(windows));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get recent aggregated metrics from Elasticsearch")
    public ResponseEntity<List<AggregatedMetrics>> getRecentMetrics(
            @RequestParam(defaultValue = "20") int count) {
        return ResponseEntity.ok(analyticsService.getRecentMetrics(count));
    }
}
