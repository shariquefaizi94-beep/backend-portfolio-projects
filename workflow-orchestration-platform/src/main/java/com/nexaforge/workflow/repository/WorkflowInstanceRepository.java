package com.nexaforge.workflow.repository;

import com.nexaforge.workflow.domain.enums.WorkflowStatus;
import com.nexaforge.workflow.domain.model.WorkflowInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {

    Optional<WorkflowInstance> findByCorrelationId(String correlationId);

    Page<WorkflowInstance> findByStatus(WorkflowStatus status, Pageable pageable);

    @Query("SELECT w FROM WorkflowInstance w WHERE w.status = :status AND w.updatedAt < :before")
    List<WorkflowInstance> findStaleWorkflows(
            @Param("status") WorkflowStatus status,
            @Param("before") Instant before
    );

    @Query("SELECT w FROM WorkflowInstance w LEFT JOIN FETCH w.steps WHERE w.id = :id")
    Optional<WorkflowInstance> findByIdWithSteps(@Param("id") UUID id);

    @Query("SELECT w FROM WorkflowInstance w LEFT JOIN FETCH w.steps LEFT JOIN FETCH w.events WHERE w.id = :id")
    Optional<WorkflowInstance> findByIdWithStepsAndEvents(@Param("id") UUID id);

    long countByStatus(WorkflowStatus status);
}
