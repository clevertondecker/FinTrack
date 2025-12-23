package com.fintrack.domain.invoice;

/**
 * Enum representing the source type of invoice import.
 */
public enum ImportSource {
    /** PDF file import. */
    PDF("PDF"),
    /** Image file import. */
    IMAGE("Imagem"),
    /** Email import. */
    EMAIL("Email"),
    /** Manual import. */
    MANUAL("Manual");

    /** The display name for the import source. */
    private final String displayName;

    ImportSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 