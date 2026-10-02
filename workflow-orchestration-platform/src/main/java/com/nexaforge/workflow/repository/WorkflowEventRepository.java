package com.nexaforge.workflow.repository;

import com.nexaforge.workflow.domain.model.WorkflowEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WorkflowEventRepository extends JpaRepository<WorkflowEvent, UUID> {

    Page<WorkflowEvent> findByWorkflowInstanceIdOrderByOccurredAtDesc(
            UUID workflowInstanceId, Pageable pageable);
}
