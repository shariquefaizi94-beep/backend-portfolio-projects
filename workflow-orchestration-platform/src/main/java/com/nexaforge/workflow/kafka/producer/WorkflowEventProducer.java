package com.nexaforge.workflow.kafka.producer;

import com.nexaforge.workflow.domain.event.WorkflowCommandEvent;
import com.nexaforge.workflow.domain.event.WorkflowStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes workflow events and commands to Kafka topics.
 *
 * <p>Uses the workflow ID as the message key to ensure ordering
 * within a single workflow across partitions.
 */
@Component
public class WorkflowEventProducer {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.workflow-commands}")
    private String commandsTopic;

    @Value("${kafka.topics.workflow-events}")
    private String eventsTopic;

    @Value("${kafka.topics.workflow-dlq}")
    private String dlqTopic;

    public WorkflowEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendCommand(WorkflowCommandEvent command) {
        String key = command.workflowId().toString();
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(commandsTopic, key, command);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send command {} for workflow {}",
                        command.commandType(), command.workflowId(), ex);
            } else {
                log.debug("Command sent: {} for workflow {} [partition={}, offset={}]",
                        command.commandType(),
                        command.workflowId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    public void sendStateEvent(WorkflowStateEvent event) {
        String key = event.workflowId().toString();
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(eventsTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send state event for workflow {}", event.workflowId(), ex);
            } else {
                log.debug("State event sent: {} → {} for workflow {}",
                        event.previousStatus(), event.newStatus(), event.workflowId());
            }
        });
    }

    public void sendToDlq(String key, Object failedMessage, String errorReason) {
        kafkaTemplate.send(dlqTopic, key, failedMessage);
        log.warn("Message sent to DLQ for key {}: {}", key, errorReason);
    }
}
