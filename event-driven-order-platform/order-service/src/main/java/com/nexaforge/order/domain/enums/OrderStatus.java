package com.nexaforge.order.domain.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Order lifecycle state machine.
 *
 * <pre>
 * PENDING → PAYMENT_PROCESSING → PAYMENT_COMPLETED → INVENTORY_RESERVED
 *           → SHIPPING_ARRANGED → COMPLETED
 *
 * Any step failure → COMPENSATING → CANCELLED
 * </pre>
 */
public enum OrderStatus {
    PENDING,
    PAYMENT_PROCESSING,
    PAYMENT_COMPLETED,
    INVENTORY_RESERVED,
    SHIPPING_ARRANGED,
    COMPLETED,
    COMPENSATING,
    CANCELLED,
    FAILED;

    public Set<OrderStatus> validTransitions() {
        return switch (this) {
            case PENDING -> EnumSet.of(PAYMENT_PROCESSING, CANCELLED);
            case PAYMENT_PROCESSING -> EnumSet.of(PAYMENT_COMPLETED, FAILED);
            case PAYMENT_COMPLETED -> EnumSet.of(INVENTORY_RESERVED, COMPENSATING);
            case INVENTORY_RESERVED -> EnumSet.of(SHIPPING_ARRANGED, COMPENSATING);
            case SHIPPING_ARRANGED -> EnumSet.of(COMPLETED, COMPENSATING);
            case FAILED -> EnumSet.of(COMPENSATING, CANCELLED);
            case COMPENSATING -> EnumSet.of(CANCELLED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus target) {
        return validTransitions().contains(target);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
