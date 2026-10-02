package com.nexaforge.workflow.service.engine.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaforge.workflow.domain.enums.StepType;
import com.nexaforge.workflow.service.engine.StepExecutionException;
import com.nexaforge.workflow.service.engine.StepExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simulates payment processing — authorization and capture.
 * In production this would integrate with a payment gateway (Stripe, etc.).
 *
 * <p>Compensation releases the payment authorization.
 */
@Component
public class ProcessPaymentExecutor implements StepExecutor {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentExecutor.class);
    private final ObjectMapper objectMapper;

    public ProcessPaymentExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public StepType getStepType() {
        return StepType.PROCESS_PAYMENT;
    }

    @Override
    public String execute(String payload) {
        log.info("Processing payment for order");
        try {
            JsonNode order = objectMapper.readTree(payload);
            double amount = order.get("totalAmount").asDouble();

            // Simulate payment gateway interaction
            // In production: call payment API, handle timeouts, verify response
            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // Simulate occasional transient failures for demonstration
            if (amount > 99999) {
                throw new StepExecutionException(
                        "Payment declined: amount exceeds limit", false);
            }

            String result = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("transactionId", transactionId)
                            .put("amount", amount)
                            .put("currency", "USD")
                            .put("status", "AUTHORIZED")
            );

            log.info("Payment authorized: {} for amount {}", transactionId, amount);
            return result;
        } catch (StepExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new StepExecutionException("Payment processing failed: " + e.getMessage(), e, true);
        }
    }

    @Override
    public void compensate(String payload, String previousOutput) {
        log.info("Compensating payment — releasing authorization");
        try {
            if (previousOutput != null) {
                JsonNode paymentResult = objectMapper.readTree(previousOutput);
                String transactionId = paymentResult.get("transactionId").asText();
                // In production: call payment gateway to void/refund the authorization
                log.info("Payment authorization released for transaction: {}", transactionId);
            }
        } catch (Exception e) {
            log.error("Failed to compensate payment", e);
        }
    }
}
