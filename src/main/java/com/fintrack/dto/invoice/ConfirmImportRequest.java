package com.fintrack.dto.invoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Request to confirm a previewed import with user-verified card mappings.
 */
public record ConfirmImportRequest(

    @NotEmpty(message = "At least one card mapping is required.")
    @Valid
    List<CardMapping> cardMappings
) {

    /**
     * Maps a detected card (by last four digits) to a registered credit card.
     */
    public record CardMapping(

        @NotNull(message = "Detected last four digits is required.")
        String detectedLastFourDigits,

        @NotNull(message = "Credit card ID is required.")
        @Positive(message = "Credit card ID must be positive.")
        Long creditCardId
    ) {}
}
