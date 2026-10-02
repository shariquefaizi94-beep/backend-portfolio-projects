package com.nexaforge.order.service;

import com.nexaforge.order.domain.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    @DisplayName("PENDING transitions to PAYMENT_PROCESSING or CANCELLED")
    void pendingTransitions() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.PAYMENT_PROCESSING)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.COMPLETED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"COMPLETED", "CANCELLED"})
    @DisplayName("Terminal states have no valid transitions")
    void terminalStates(OrderStatus status) {
        assertThat(status.isTerminal()).isTrue();
        assertThat(status.validTransitions()).isEmpty();
    }

    @Test
    @DisplayName("PAYMENT_COMPLETED transitions to INVENTORY_RESERVED or COMPENSATING")
    void paymentCompletedTransitions() {
        assertThat(OrderStatus.PAYMENT_COMPLETED.canTransitionTo(OrderStatus.INVENTORY_RESERVED)).isTrue();
        assertThat(OrderStatus.PAYMENT_COMPLETED.canTransitionTo(OrderStatus.COMPENSATING)).isTrue();
        assertThat(OrderStatus.PAYMENT_COMPLETED.canTransitionTo(OrderStatus.PENDING)).isFalse();
    }
}
