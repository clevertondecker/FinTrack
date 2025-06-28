package com.fintrack.domain.invoice;

/**
 * Enum representing the status of an invoice import process.
 */
public enum ImportStatus {
    PENDING("Pendente"),
    PROCESSING("Processando"),
    COMPLETED("Concluído"),
    FAILED("Falhou"),
    MANUAL_REVIEW("Revisão Manual");

    private final String displayName;

    ImportStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 