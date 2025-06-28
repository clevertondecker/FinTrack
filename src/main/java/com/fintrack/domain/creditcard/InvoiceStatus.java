package com.fintrack.domain.creditcard;

/**
 * Enum representing the status of a credit card invoice.
 * Defines the possible states an invoice can be in.
 */
public enum InvoiceStatus {

    /**
     * Invoice is open and pending payment.
     */
    OPEN("Open"),

    /**
     * Invoice has been fully paid.
     */
    PAID("Paid"),

    /**
     * Invoice is overdue and past the due date.
     */
    OVERDUE("Overdue"),

    /**
     * Invoice has been partially paid.
     */
    PARTIAL("Partial"),

    /**
     * Invoice is closed - no amount to pay and past due date.
     */
    CLOSED("Closed");

    private final String displayName;

    InvoiceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
} 