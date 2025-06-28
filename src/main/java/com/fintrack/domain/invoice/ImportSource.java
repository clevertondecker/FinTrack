package com.fintrack.domain.invoice;

/**
 * Enum representing the source type of an invoice import.
 */
public enum ImportSource {
    PDF("PDF"),
    IMAGE("Imagem"),
    EMAIL("Email"),
    MANUAL("Manual");

    private final String displayName;

    ImportSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 