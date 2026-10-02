package com.nexaforge.workflow.controller;

import com.nexaforge.workflow.controller.dto.CreateWorkflowRequest;
import com.nexaforge.workflow.controller.dto.WorkflowResponse;
import com.nexaforge.workflow.controller.dto.WorkflowStatsResponse;
import com.nexaforge.workflow.domain.enums.WorkflowStatus;
import com.nexaforge.workflow.domain.model.WorkflowInstance;
import com.nexaforge.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API for workflow lifecycle management.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/workflows — Create a new workflow (idempotent)</li>
 *   <li>GET  /api/v1/workflows/{id} — Get workflow by ID</li>
 *   <li>GET  /api/v1/workflows — List workflows with pagination and filtering</li>
 *   <li>POST /api/v1/workflows/{id}/cancel — Cancel a running workflow</li>
 *   <li>GET  /api/v1/workflows/stats — Get workflow statistics</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflow", description = "Workflow lifecycle management APIs")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new order fulfillment workflow",
            description = "Creates and starts a new workflow. Idempotent — the same correlationId returns the existing workflow."
    )
    @ApiResponse(responseCode = "201", description = "Workflow created and started")
    @ApiResponse(responseCode = "200", description = "Existing workflow returned (duplicate correlationId)")
    public ResponseEntity<WorkflowResponse> createWorkflow(
            @Valid @RequestBody CreateWorkflowRequest request) {

        WorkflowInstance workflow = workflowService.createOrderFulfillmentWorkflow(
                request.correlationId(), request.payload());

        HttpStatus status = workflow.getCreatedAt().plusMillis(500)
                .isAfter(java.time.Instant.now())
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status).body(WorkflowResponse.from(workflow));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow by ID")
    @ApiResponse(responseCode = "200", description = "Workflow found")
    @ApiResponse(responseCode = "404", description = "Workflow not found")
    public ResponseEntity<WorkflowResponse> getWorkflow(
            @PathVariable UUID id) {
        WorkflowInstance workflow = workflowService.getWorkflow(id);
        return ResponseEntity.ok(WorkflowResponse.from(workflow));
    }

    @GetMapping
    @Operation(summary = "List workflows with optional status filter")
    public ResponseEntity<Page<WorkflowResponse>> listWorkflows(
            @Parameter(description = "Filter by workflow status")
            @RequestParam(required = false) WorkflowStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<WorkflowResponse> page = workflowService.listWorkflows(status, pageable)
                .map(WorkflowResponse::from);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a running workflow")
    @ApiResponse(responseCode = "200", description = "Workflow cancelled")
    @ApiResponse(responseCode = "409", description = "Workflow already in terminal state")
    public ResponseEntity<WorkflowResponse> cancelWorkflow(@PathVariable UUID id) {
        WorkflowInstance workflow = workflowService.cancelWorkflow(id);
        return ResponseEntity.ok(WorkflowResponse.from(workflow));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get workflow statistics")
    public ResponseEntity<WorkflowStatsResponse> getStats() {
        WorkflowService.WorkflowStats stats = workflowService.getStats();
        return ResponseEntity.ok(new WorkflowStatsResponse(
                stats.total(), stats.pending(), stats.inProgress(),
                stats.completed(), stats.failed(), stats.compensated()
        ));
    }
}
