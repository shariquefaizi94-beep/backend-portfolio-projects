package com.nexaforge.workflow.controller.dto;

/**
 * Response DTO for workflow statistics dashboard.
 */
public record WorkflowStatsResponse(
        long total,
        long pending,
        long inProgress,
        long completed,
        long failed,
        long compensated
) {}
