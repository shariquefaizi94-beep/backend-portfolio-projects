-- Workflow Orchestration Platform — Schema v1
-- Supports: workflow instances, steps, audit events

CREATE TABLE workflow_instances (
    id                UUID PRIMARY KEY,
    correlation_id    VARCHAR(128) NOT NULL UNIQUE,
    workflow_type     VARCHAR(64) NOT NULL,
    status            VARCHAR(32) NOT NULL,
    version           BIGINT NOT NULL DEFAULT 0,
    current_step_index INT NOT NULL DEFAULT 0,
    retry_count       INT NOT NULL DEFAULT 0,
    max_retries       INT NOT NULL DEFAULT 3,
    payload           TEXT,
    failure_reason    VARCHAR(1024),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE,
    completed_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_workflow_status ON workflow_instances(status);
CREATE INDEX idx_workflow_correlation_id ON workflow_instances(correlation_id);
CREATE INDEX idx_workflow_created_at ON workflow_instances(created_at);
CREATE INDEX idx_workflow_updated_at ON workflow_instances(updated_at);

CREATE TABLE workflow_steps (
    id                   UUID PRIMARY KEY,
    workflow_instance_id UUID NOT NULL REFERENCES workflow_instances(id) ON DELETE CASCADE,
    step_type            VARCHAR(32) NOT NULL,
    status               VARCHAR(32) NOT NULL,
    step_order           INT NOT NULL,
    retry_count          INT NOT NULL DEFAULT 0,
    max_retries          INT NOT NULL DEFAULT 3,
    input                TEXT,
    output               TEXT,
    error_message        VARCHAR(1024),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at           TIMESTAMP WITH TIME ZONE,
    completed_at         TIMESTAMP WITH TIME ZONE,
    duration_ms          BIGINT
);

CREATE INDEX idx_step_workflow_id ON workflow_steps(workflow_instance_id);
CREATE INDEX idx_step_status ON workflow_steps(status);

CREATE TABLE workflow_events (
    id                   UUID PRIMARY KEY,
    workflow_instance_id UUID NOT NULL REFERENCES workflow_instances(id) ON DELETE CASCADE,
    event_type           VARCHAR(64) NOT NULL,
    detail               TEXT,
    occurred_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_event_workflow_id ON workflow_events(workflow_instance_id);
CREATE INDEX idx_event_occurred_at ON workflow_events(occurred_at);
CREATE INDEX idx_event_type ON workflow_events(event_type);
