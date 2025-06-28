package com.fintrack.domain.creditcard;

/**
 * Enum representing different payment methods for shared debts.
 */
public enum PaymentMethod {
    PIX("PIX"),
    BANK_TRANSFER("Transferência Bancária"),
    CASH("Dinheiro"),
    CREDIT_CARD("Cartão de Crédito"),
    DEBIT_CARD("Cartão de Débito"),
    OTHER("Outro");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 