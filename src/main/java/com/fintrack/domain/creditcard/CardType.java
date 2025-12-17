package com.fintrack.domain.creditcard;

/**
 * Enum representing different types of credit cards.
 */
public enum CardType {
    PHYSICAL("Físico"),
    VIRTUAL("Virtual"),
    ADDITIONAL("Adicional");

    private final String displayName;

    CardType(String displayName) {
        this.displayName = displayName;
    }
} 