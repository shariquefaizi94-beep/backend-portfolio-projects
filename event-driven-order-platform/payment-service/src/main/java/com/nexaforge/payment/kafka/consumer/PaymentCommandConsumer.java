package com.nexaforge.payment.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaforge.payment.domain.model.Payment;
import com.nexaforge.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Listens for OrderCreated events and processes payment.
 * Publishes PaymentCompleted or PaymentFailed back to Kafka.
 */
@Component
public class PaymentCommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandConsumer.class);

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.payment-events}")
    private String paymentEventsTopic;

    public PaymentCommandConsumer(PaymentService paymentService,
                                  KafkaTemplate<String, Object> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topics.order-events}", groupId = "payment-service")
    public void handleOrderCreated(String record, org.apache.kafka.clients.consumer.ConsumerRecord<String, String> raw, Acknowledgment ack) {
        try {
            JsonNode event = objectMapper.readTree(record);
            String orderId = event.path("orderId").asText();
            String customerId = event.path("customerId").asText();
            BigDecimal amount = new BigDecimal(event.path("totalAmount").asText());
            String currency = event.path("currency").asText("USD");

            MDC.put("correlationId", event.path("correlationId").asText());
            log.info("Processing payment for order {}, amount {}", orderId, amount);

            Payment payment = paymentService.processPayment(orderId, customerId, amount, currency);

            if ("CAPTURED".equals(payment.getStatus()) || "AUTHORIZED".equals(payment.getStatus())) {
                publishPaymentCompleted(orderId, payment);
            } else {
                publishPaymentFailed(orderId, payment.getFailureReason());
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process payment", e);
            ack.acknowledge();
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(topics = "${kafka.topics.compensation-events}", groupId = "payment-compensation")
    public void handleCompensation(String record, Acknowledgment ack) {
        try {
            JsonNode event = objectMapper.readTree(record);
            String orderId = event.path("orderId").asText();
            log.info("Processing payment compensation (refund) for order {}", orderId);
            paymentService.refundPayment(orderId);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process payment compensation", e);
            ack.acknowledge();
        }
    }

    private void publishPaymentCompleted(String orderId, Payment payment) {
        var event = objectMapper.createObjectNode()
                .put("orderId", orderId)
                .put("transactionId", payment.getTransactionId())
                .put("amount", payment.getAmount().toString())
                .put("status", "COMPLETED")
                .put("correlationId", orderId);
        kafkaTemplate.send(paymentEventsTopic, orderId, event.toString());
    }

    private void publishPaymentFailed(String orderId, String reason) {
        var event = objectMapper.createObjectNode()
                .put("orderId", orderId)
                .put("reason", reason)
                .put("correlationId", orderId);
        kafkaTemplate.send(paymentEventsTopic, orderId, event.toString());
    }
}
