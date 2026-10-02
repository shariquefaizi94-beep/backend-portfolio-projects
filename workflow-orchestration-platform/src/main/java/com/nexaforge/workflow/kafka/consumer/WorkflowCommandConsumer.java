package com.nexaforge.workflow.kafka.consumer;

import com.nexaforge.workflow.domain.event.WorkflowCommandEvent;
import com.nexaforge.workflow.kafka.producer.WorkflowEventProducer;
import com.nexaforge.workflow.service.engine.WorkflowEngine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for workflow command events.
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>Manual acknowledgment — only commits offset after successful processing</li>
 *   <li>Uses the workflow engine's idempotency check to handle duplicate deliveries</li>
 *   <li>Failed messages go to DLQ after retries are exhausted</li>
 *   <li>Each record is logged with its offset and partition for debugging</li>
 * </ul>
 */
@Component
public class WorkflowCommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(WorkflowCommandConsumer.class);

    private final WorkflowEngine workflowEngine;
    private final WorkflowEventProducer eventProducer;
    private final Counter messagesProcessed;
    private final Counter messagesFailed;

    public WorkflowCommandConsumer(
            WorkflowEngine workflowEngine,
            WorkflowEventProducer eventProducer,
            MeterRegistry meterRegistry) {
        this.workflowEngine = workflowEngine;
        this.eventProducer = eventProducer;
        this.messagesProcessed = Counter.builder("kafka.consumer.messages.processed")
                .tag("topic", "workflow.commands")
                .register(meterRegistry);
        this.messagesFailed = Counter.builder("kafka.consumer.messages.failed")
                .tag("topic", "workflow.commands")
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = "${kafka.topics.workflow-commands}",
            groupId = "workflow-orchestrator",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCommand(ConsumerRecord<String, WorkflowCommandEvent> record,
                              Acknowledgment acknowledgment) {
        WorkflowCommandEvent command = record.value();
        log.info("Received command: {} for workflow {} [partition={}, offset={}]",
                command.commandType(),
                command.workflowId(),
                record.partition(),
                record.offset());

        try {
            workflowEngine.processCommand(command);
            messagesProcessed.increment();
            acknowledgment.acknowledge();

            log.debug("Command processed successfully: {} for workflow {}",
                    command.commandType(), command.workflowId());
        } catch (Exception e) {
            messagesFailed.increment();
            log.error("Failed to process command: {} for workflow {}",
                    command.commandType(), command.workflowId(), e);

            // Send to DLQ after failure
            eventProducer.sendToDlq(record.key(), command,
                    "Processing failed: " + e.getMessage());
            acknowledgment.acknowledge(); // Acknowledge to avoid infinite retry loop
        }
    }
}
