package com.nexaforge.workflow.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Creates Kafka topics on application startup.
 * Partition count and replication factor are configured for a local dev environment.
 * In production these would be higher (e.g., 12 partitions, RF=3).
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.workflow-commands}")
    private String commandsTopic;

    @Value("${kafka.topics.workflow-events}")
    private String eventsTopic;

    @Value("${kafka.topics.workflow-dlq}")
    private String dlqTopic;

    @Value("${kafka.topics.compensation-commands}")
    private String compensationTopic;

    @Bean
    public NewTopic workflowCommandsTopic() {
        return TopicBuilder.name(commandsTopic)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic workflowEventsTopic() {
        return TopicBuilder.name(eventsTopic)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic workflowDlqTopic() {
        return TopicBuilder.name(dlqTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic compensationCommandsTopic() {
        return TopicBuilder.name(compensationTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
