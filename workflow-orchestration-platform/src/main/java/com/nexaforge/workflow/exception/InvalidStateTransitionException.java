package com.nexaforge.workflow.exception;

/**
 * Thrown when an invalid workflow state transition is attempted.
 * Indicates a programming error or an unexpected concurrent modification.
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
