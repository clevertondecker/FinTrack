package com.fintrack.domain.creditcard;

/**
 * Enum representing different types of credit cards.
 */
public enum CardType {
    /** Physical credit card. */
    PHYSICAL("Físico"),
    /** Virtual credit card. */
    VIRTUAL("Virtual"),
    /** Additional credit card. */
    ADDITIONAL("Adicional");

    /** The display name for the card type. */
    private final String displayName;

    CardType(String displayName) {
        this.displayName = displayName;
    }
} 