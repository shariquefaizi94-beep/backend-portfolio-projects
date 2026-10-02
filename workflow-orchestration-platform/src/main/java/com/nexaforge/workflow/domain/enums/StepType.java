package com.nexaforge.workflow.domain.enums;

/**
 * Defines the types of steps in the order fulfillment workflow.
 * Each step maps to a specific business action in the fulfillment pipeline.
 */
public enum StepType {

    VALIDATE_ORDER("Validate order details and business rules"),
    PROCESS_PAYMENT("Process payment authorization"),
    RESERVE_INVENTORY("Reserve inventory for order items"),
    ARRANGE_SHIPPING("Create shipping arrangement and label"),
    SEND_NOTIFICATION("Send order confirmation notification"),
    COMPLETE_ORDER("Finalize the order and update records");

    private final String description;

    StepType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
