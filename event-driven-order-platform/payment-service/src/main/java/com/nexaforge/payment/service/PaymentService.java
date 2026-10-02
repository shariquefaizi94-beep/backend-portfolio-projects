package com.nexaforge.payment.service;

import com.nexaforge.payment.domain.model.Payment;
import com.nexaforge.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Simulates payment gateway interaction.
 * Idempotent: returns existing payment if orderId already processed.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment processPayment(String orderId, String customerId, BigDecimal amount, String currency) {
        // Idempotency — check if already processed
        return paymentRepository.findByOrderId(orderId)
                .map(existing -> {
                    log.info("Payment already processed for order {}: {}", orderId, existing.getTransactionId());
                    return existing;
                })
                .orElseGet(() -> {
                    // Simulate payment processing
                    if (amount.compareTo(new BigDecimal("99999")) > 0) {
                        Payment failed = Payment.create(orderId, customerId, amount, currency);
                        failed.fail("Amount exceeds limit");
                        return paymentRepository.save(failed);
                    }

                    Payment payment = Payment.create(orderId, customerId, amount, currency);
                    payment.capture();
                    Payment saved = paymentRepository.save(payment);
                    log.info("Payment captured: {} for order {} amount {}", saved.getTransactionId(), orderId, amount);
                    return saved;
                });
    }

    @Transactional
    public void refundPayment(String orderId) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (!"REFUNDED".equals(payment.getStatus())) {
                payment.refund();
                paymentRepository.save(payment);
                log.info("Payment refunded for order {}: {}", orderId, payment.getTransactionId());
            }
        });
    }
}
