package com.fintrack.domain.invoice;

/**
 * Enum representing the status of an invoice import process.
 */
public enum ImportStatus {
    /** Import is pending processing. */
    PENDING("Pendente"),
    /** Import is being processed. */
    PROCESSING("Processando"),
    /** Import completed successfully. */
    COMPLETED("Concluído"),
    /** Import failed. */
    FAILED("Falhou"),
    /** Import requires manual review. */
    MANUAL_REVIEW("Revisão Manual");

    /** The display name for the import status. */
    private final String displayName;

    ImportStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 